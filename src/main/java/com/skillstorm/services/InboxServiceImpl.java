package com.skillstorm.services;

import com.skillstorm.dtos.InboxDto;
import com.skillstorm.entities.Inbox;
import com.skillstorm.repositories.InboxRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class InboxServiceImpl implements InboxService {

    private final InboxRepository inboxRepository;

    @Autowired
    public InboxServiceImpl(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    // Post message to User's inbox:
    @RabbitListener(queues = "inbox-queue")
    public Mono<Void> postMessageToInboxByUsername(@Payload InboxDto inbox) {
        System.out.println("\n\nReceived message: " + inbox.toString());
        return inboxRepository.save(inbox.mapToEntity())
                .then();
    }

    // Get all Forms awaiting a User's approval:
    @Override
    public Flux<UUID> getAllAwaitingApprovalByUsername(String username) {
        return inboxRepository.findAllById(username)
                .map(Inbox::getFormId);
    }
}
