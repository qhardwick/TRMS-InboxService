package com.skillstorm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageServiceImpl implements MessageService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;

    // Set up Kineses. Just to get hands on. Will need to recreate the stream in AWS console to enable this:
    private final KinesisAsyncClient kinesisClient;
    private final String streamName;

    // Set up our map to hold the list of messages for each user. Concerns about scaling:
    private final Map<String, Sinks.Many<ApprovalRequestDto>> userSinks = new ConcurrentHashMap<>();

    // Set up cache for continuous polling for new messages without having to hit the actual database:
    private final AsyncLoadingCache<String, ApprovalRequestDto> approvalRequestsCache;

    @Autowired
    public MessageServiceImpl(ApprovalRequestRepository approvalRequestRepository, VerificationRequestRepository verificationRequestRepository, RabbitTemplate rabbitTemplate, ObjectMapper mapper,
                              KinesisAsyncClient kinesisClient, @Value("${kinesis.stream-name}") String streamName, @Lazy AsyncLoadingCache<String, ApprovalRequestDto> approvalRequestsCache) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.approvalRequestsCache = approvalRequestsCache;
        this.kinesisClient = kinesisClient;
        this.streamName = streamName;
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

     // Post ApprovalRequest to User's inbox. Caching version:
    @RabbitListener(queues = "approval-request-queue")
    //@CachePut(value = "approvalRequests", key = "#approvalRequest.username.concat('-').concat(#approvalRequest.formId.toString())")
    public Mono<Void> postApprovalRequestToInboxByUsername(@Payload ApprovalRequestDto approvalRequest) {

        // Set the approval deadline (20 seconds from now). May remove the timeCreated field in the future since we're only
        // really using it to create the deadline, but it may be something people would want tracked:
        approvalRequest.setTimeCreated(LocalDateTime.now());
        approvalRequest.setApprovalDeadline(approvalRequest.getTimeCreated().plusSeconds(20));

        return approvalRequestRepository.save(approvalRequest.mapToEntity())
                .doOnSuccess(this::publishApprovalRequestToKinesis)
                .then();
    }

    private void  publishApprovalRequestToKinesis(ApprovalRequest approvalRequest) {
        try {
            System.out.println("\nPublishing new message to kinesis: " + approvalRequest.toString());
            String jsonData = mapper.writeValueAsString(new ApprovalRequestDto(approvalRequest));

        PutRecordRequest putRequest = PutRecordRequest.builder()
                .streamName(streamName)
                .partitionKey(approvalRequest.getUsername())
                .data(SdkBytes.fromUtf8String(jsonData))
                .build();

        kinesisClient.putRecord(putRequest);

        } catch ( JsonProcessingException e) {
            System.err.println("Publish message failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Return all db entries. Just for testing
    // TODO: Delete when no longer needed
    @Override
    public Flux<ApprovalRequestDto> findAll() {
        return approvalRequestRepository.findAll()
                .map(ApprovalRequestDto::new);
    }

    @Override
    @Cacheable(value = "approvalRequests", key = "#username.concat('-').concat(#formId.toString())")
    public Mono<ApprovalRequestDto> getMessageByUsernameAndFormId(String username, UUID formId) {
        return approvalRequestRepository.findByUsernameAndFormId(username, formId)
                .map(ApprovalRequestDto::new);
    }

    // Get all Forms awaiting a User's approval:
    @Override
    public Flux<ApprovalRequestDto> getAllAwaitingApprovalByUsername(String username) {
        // Gather all keys for this user from the cache
        return Flux.fromIterable(approvalRequestsCache.asMap().keySet())
                .filter(key -> key.startsWith(username + "-"))
                .flatMap(key -> Flux.from(Mono.fromFuture(approvalRequestsCache.get(key))))
                .switchIfEmpty(approvalRequestRepository.findAllByUsername(username)
                        .map(ApprovalRequestDto::new));
    }

    // Check the cache to see if the user has any new messages. If so, load them into the corresponding sink:
    public Flux<ApprovalRequestDto> getApprovalRequestUpdates(String username) {
        return userSinks.computeIfAbsent(username, k -> {
            Sinks.Many<ApprovalRequestDto> sink = Sinks.many().replay().latest();
            startListeningForNewApprovalMessages(username, sink);
            return sink;
        }).asFlux();
    }

    // Start querying the cache every second to look for changes:
    private void startListeningForNewApprovalMessages(String username, Sinks.Many<ApprovalRequestDto> sink) {
        // Keep track of the last emitted message for each user to detect changes
        final List<ApprovalRequestDto> lastEmitted = new ArrayList<>();

        Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> approvalRequestRepository.findAllByUsername(username)
                        .map(ApprovalRequestDto::new)
                        .collectList())
                .flatMapIterable(list -> {
                    // Compare the new list with the last emitted list
                    List<ApprovalRequestDto> newMessages = new ArrayList<>();
                    for (ApprovalRequestDto dto : list) {
                        if(!lastEmitted.contains(dto)) {
                            newMessages.add(dto);
                        }
                    }
                    // Update the last emitted list
                    lastEmitted.clear();
                    lastEmitted.addAll(list);
                    return newMessages;
                })
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
    @CacheEvict(value = "approvalRequests", key = "#approvalRequest.username.concat('-').concat(#approvalRequest.formId.toString())")
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
