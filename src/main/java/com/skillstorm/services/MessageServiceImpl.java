package com.skillstorm.services;

import com.skillstorm.constants.Queues;
import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.entities.ApprovalRequest;
import com.skillstorm.repositories.ApprovalRequestRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public MessageServiceImpl(ApprovalRequestRepository approvalRequestRepository, RabbitTemplate rabbitTemplate) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.rabbitTemplate = rabbitTemplate;

        startScheduler().subscribe();
    }

    // Post ApprovalRequest to User's inbox:
    @RabbitListener(queues = "approval-request-queue")
    public Mono<Void> postApprovalRequestToInboxByUsername(@Payload ApprovalRequestDto approvalRequest) {
        approvalRequest.setTimeCreated(LocalDateTime.now());
        approvalRequest.setApprovalDeadline(
                approvalRequest.getTimeCreated().plusSeconds(20)
        );
        return approvalRequestRepository.save(approvalRequest.mapToEntity())
                .then();
    }

    // Set up a scheduled task to run periodically to check for stale approval requests. We're just testing here, so we want the
    // task to run frequently to ensure that everything works as expected. We're running it every 30 seconds:
    private Flux<Void> startScheduler() {
        return Flux.interval(Duration.ofSeconds(30))
                .onBackpressureDrop()
                .flatMap(tick -> checkForApprovalDeadlines());
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
}
