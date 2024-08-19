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
public class MessageServiceImpl implements MessageService {

    private final InboxRepository inboxRepository;

    @Autowired
    public MessageServiceImpl(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    // Post message to User's inbox:
    @RabbitListener(queues = "inbox-queue")
    public Mono<Void> postMessageToInboxByUsername(@Payload InboxDto inbox) {
        System.out.println("\n\nReceived message: " + inbox.toString());
        return inboxRepository.save(inbox.mapToEntity())
                .then();
    }

    // Return all entries in db. Just for testing:
    @Override
    public Flux<InboxDto> findAll() {
        return inboxRepository.findAll()
                .map(InboxDto::new);
    }

    // Get all Forms awaiting a User's approval:
    @Override
    public Flux<UUID> getAllAwaitingApprovalByUsername(String username) {
        return inboxRepository.findAllById(username)
                .map(Inbox::getFormId);
    }
}
