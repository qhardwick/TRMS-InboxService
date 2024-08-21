package com.skillstorm.services;

import com.skillstorm.constants.Queues;
import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.entities.ApprovalRequest;
import com.skillstorm.repositories.ApprovalRequestRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

        startScheduler();
    }

    // Post ApprovalRequest to User's inbox:
    @RabbitListener(queues = "approval-request-queue")
    public Mono<Void> postApprovalRequestToInboxByUsername(@Payload ApprovalRequestDto approvalRequest) {
        approvalRequest.setTimeCreated(LocalDateTime.now());
        approvalRequest.setApprovalDeadline(
                approvalRequest.getTimeCreated().plusSeconds(20)
        );
        return approvalRequestRepository.save(approvalRequest.mapToEntity())
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    // Set up a scheduled task to run periodically to check for stale approval requests. We're just testing here, so we want the
    // task to run frequently to ensure that everything works as expected. We're running it every 30 seconds:
    private void startScheduler() {
        Flux.interval(Duration.ofSeconds(30))
                .onBackpressureDrop()
                .map(tick -> checkForApprovalDeadlines())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    public Mono<Void> checkForApprovalDeadlines() {
        approvalRequestRepository.findAll().filter(approvalRequest -> LocalDateTime.now().isAfter(approvalRequest.getApprovalDeadline()))
                .map(this::submitForAutoApproval);
    }

    // Submit to Form-Service for auto-approval:
    private Mono<Void> submitForAutoApproval(ApprovalRequest approvalRequest) {
        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(Queues.AUTO_APPROVAL.toString(), new ApprovalRequestDto(approvalRequest)))
                .then();
    }

    // Delete Message from User's inbox:
    @RabbitListener(queues = "deletion-request-queue")
    public Mono<Void> deleteMessageFromInbox(@Payload ApprovalRequestDto message) {
        return approvalRequestRepository.delete(message.mapToEntity())
                .then();
    }

    // Return all entries in db. Just for testing:
    @Override
    public Flux<ApprovalRequestDto> findAll() {
        return approvalRequestRepository.findAll()
                .map(ApprovalRequestDto::new);
    }

    // Get all Forms awaiting a User's approval:
    @Override
    public Flux<UUID> getAllAwaitingApprovalByUsername(String username) {
        return approvalRequestRepository.findAllById(username)
                .map(ApprovalRequest::getFormId);
    }
}
