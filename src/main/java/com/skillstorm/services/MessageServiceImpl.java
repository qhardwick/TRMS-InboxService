package com.skillstorm.services;

import com.rabbitmq.client.Channel;
import com.skillstorm.constants.Queues;
import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.dtos.VerificationRequestDto;
import com.skillstorm.entities.ApprovalRequest;
import com.skillstorm.repositories.ApprovalRequestRepository;
import com.skillstorm.repositories.VerificationRequestRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public MessageServiceImpl(ApprovalRequestRepository approvalRequestRepository, VerificationRequestRepository verificationRequestRepository, RabbitTemplate rabbitTemplate) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Post ApprovalRequest to User's inbox:
    @RabbitListener(queues = "approval-request-queue")
    public Mono<Void> postApprovalRequestToInboxByUsername(@Payload ApprovalRequestDto approvalRequest, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        approvalRequest.setTimeCreated(LocalDateTime.now());

        // Set the approval deadline (20 seconds from now)
        approvalRequest.setApprovalDeadline(approvalRequest.getTimeCreated().plusSeconds(20));

        return approvalRequestRepository.save(approvalRequest.mapToEntity())
                .flatMap(ignored -> handleAcknowledgement(channel, deliveryTag)) // Successfully acknowledge
                .onErrorResume(error -> {
                    // If there's an error, handle it by negatively acknowledging the message
                    return handleNegativeAcknowledgement(error, channel, deliveryTag);
                });
    }


    // Set up a scheduled task to run periodically to check for stale approval requests. We're just testing here, so we want the
    // task to run frequently for testing to ensure that everything works as expected. We're running it every 30 seconds:
    @PostConstruct
    private void startScheduler() {
        Flux.interval(Duration.ofSeconds(30))
                .onBackpressureDrop()
                .flatMap(tick -> checkForApprovalDeadlines())
                .subscribe();
    }

    // Query the repository for all ApprovalRequests past their deadlines:
    public Flux<Void> checkForApprovalDeadlines() {
        return approvalRequestRepository.findAllRequestsWithExpiredDeadlines(LocalDateTime.now())
                .flatMap(this::submitForAutoApproval);
    }

    // Submit to Form-Service for auto-approval:
    private Mono<Void> submitForAutoApproval(ApprovalRequest approvalRequest) {
        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(Queues.AUTO_APPROVAL.toString(), new ApprovalRequestDto(approvalRequest)))
                .then();
    }

    // Delete ApprovalRequest from User's inbox:
    @RabbitListener(queues = "deletion-request-queue")
    public Mono<Void> deleteMessageFromInbox(@Payload ApprovalRequestDto approvalRequest) {
        return approvalRequestRepository
                .deleteByUsernameAndFormId(approvalRequest.getUsername(), approvalRequest.getFormId());
    }

    // Delete by username and FormId (controller method):
    @Override
    public Mono<Void> deleteByUsernameAndFormId(ApprovalRequestDto approvalRequest) {
        return approvalRequestRepository
                .deleteByUsernameAndFormId(approvalRequest.getUsername(), approvalRequest.getFormId());
    }

    // Add Completion Verification request to approver's inbox:
    @RabbitListener(queues = "completion-verification-queue")
    public Mono<Void> addCompletionVerificationRequest(@Payload VerificationRequestDto verificationRequest) {
        return verificationRequestRepository.save(verificationRequest.mapToEntity())
                .then();
    }

    // Return all db entries. Just for testing
    // TODO: Delete when no longer needed
    @Override
    public Flux<ApprovalRequestDto> findAll() {
        return approvalRequestRepository.findAll()
                .map(ApprovalRequestDto::new);
    }

    // Get all Forms awaiting a User's approval:
    @Override
    public Flux<UUID> getAllAwaitingApprovalByUsername(String username) {
        return approvalRequestRepository.findAllByUsername(username)
                .map(ApprovalRequestDto::new)
                .map(ApprovalRequestDto::getFormId);
    }

    // Utility method to handle acknowledgement of message receipt for all listener methods:
    private Mono<Void> handleAcknowledgement(Channel channel, long deliveryTag) {
        return Mono.fromRunnable(() -> {
            try {
                channel.basicAck(deliveryTag, false); // Acknowledge message
            } catch (IOException e) {
                throw new RuntimeException("Failed to acknowledge message", e);
            }
        }).then();
    }


    // Utility method to handle message not received due to error:
    private Mono<Void> handleNegativeAcknowledgement(Throwable error, Channel channel, long deliveryTag) {
        return Mono.fromRunnable(() -> {
            try {
                // Reject the message, and requeue it
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException e) {
                throw new RuntimeException("Failed to negatively acknowledge message", e);
            }
        }).then();
    }

}
