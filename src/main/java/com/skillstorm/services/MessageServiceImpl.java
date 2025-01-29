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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class MessageServiceImpl implements MessageService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final RabbitTemplate rabbitTemplate;

    // Set up cache for continuous polling for new messages without having to hit the actual database:
    private final Map<String, Queue<ApprovalRequestDto>> approvalRequestsCache = new ConcurrentHashMap<>();

    // Alternatively, utilize Kinesis data streams to publish real-time updates:
    private final KinesisService kinesisService;

    @Autowired
    public MessageServiceImpl(ApprovalRequestRepository approvalRequestRepository, VerificationRequestRepository verificationRequestRepository,
                              RabbitTemplate rabbitTemplate, KinesisService kinesisService) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.kinesisService = kinesisService;
    }

    // Set up a scheduled task to run periodically to check for stale approval requests. We're just testing here, so we want the
    // task to run frequently for testing to ensure that everything works as expected. We're running it every 30 seconds:
    @PostConstruct
    private void startScheduler() {
        Flux.interval(Duration.ofSeconds(300000))
                .onBackpressureDrop()
                .flatMap(tick -> checkForApprovalDeadlines())
                .subscribe();
    }

     // Post ApprovalRequest to User's inbox. Caching version:
    @RabbitListener(queues = "approval-request-queue")
    public Mono<Void> postApprovalRequestToInbox(@Payload ApprovalRequestDto approvalRequest) {

        // Set the approval deadline (20 seconds from now). May remove the timeCreated field in the future since we're only
        // really using it to create the deadline, but it may be something people would want tracked:
        approvalRequest.setTimeCreated(LocalDateTime.now());
        approvalRequest.setApprovalDeadline(approvalRequest.getTimeCreated().plusSeconds(20));

        return approvalRequestRepository.save(approvalRequest.mapToEntity())
                .map(ApprovalRequestDto::new)
                //.doOnSuccess(kinesisService::publishApprovalRequestToKinesis)
                .doOnSuccess(this::addRequestToCache)
                .then();
    }

    // When we create a new entry for a user, add it to the cache:
    private void addRequestToCache(ApprovalRequestDto approvalRequest) {
        approvalRequestsCache
                .computeIfAbsent(approvalRequest.getUsername(), k -> new LinkedBlockingQueue<>())
                .offer(approvalRequest);
    }

    // Get all Forms awaiting a User's approval:
    @Override
    public Flux<ApprovalRequestDto> getApprovalRequestsByUsername(String username) {
        return approvalRequestRepository.findAllByUsername(username.toLowerCase())
                .map(ApprovalRequestDto::new);
    }

    // Check the cache to see if the user has any new messages. If so, emit them and remove them from the cache:
    @Override
    public Flux<ApprovalRequestDto> getApprovalRequestUpdates(String username) {
        Queue<ApprovalRequestDto> userMessages = approvalRequestsCache.get(username.toLowerCase());

        if(userMessages == null || userMessages.isEmpty()) {
            return Flux.empty();
        }

        return Flux.create(sink -> {
            while(!userMessages.isEmpty()) {
                ApprovalRequestDto message = userMessages.poll();
                sink.next(message);
            }
            sink.complete();
        });
    }

    // Return all db entries. Just for testing
    // TODO: Delete when no longer needed
    @Override
    public Flux<ApprovalRequestDto> findAll() {
        return approvalRequestRepository.findAll()
                .map(ApprovalRequestDto::new);
    }

    @Override
    public Mono<ApprovalRequestDto> getMessageByUsernameAndFormId(String username, UUID formId) {
        return approvalRequestRepository.findByUsernameAndFormId(username.toLowerCase(), formId)
                .map(ApprovalRequestDto::new);
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
    public Mono<Void> deleteByUsernameAndFormId(@Payload ApprovalRequestDto approvalRequest) {
        return approvalRequestRepository
                .deleteByUsernameAndFormId(approvalRequest.getUsername(), approvalRequest.getFormId());
    }

    // Add Completion Verification request to approver's inbox:
    @RabbitListener(queues = "completion-verification-queue")
    public Mono<Void> addCompletionVerificationRequest(@Payload VerificationRequestDto verificationRequest) {
        return verificationRequestRepository.save(verificationRequest.mapToEntity())
                .then();
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
