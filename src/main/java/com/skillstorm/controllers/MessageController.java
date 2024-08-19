package com.skillstorm.controllers;

import com.skillstorm.dtos.InboxDto;
import com.skillstorm.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Test endpoint:
    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello Message Service");
    }

    // Testing utility:
    @GetMapping
    public Flux<InboxDto> findAllMessages() {
        return messageService.findAll();
    }

    // View Forms awaiting the User's approval:
    @GetMapping("/pending-my-approval")
    public Flux<UUID> getAllAwaitingApprovalByUsername(@RequestHeader("username") String username) {
        return messageService.getAllAwaitingApprovalByUsername(username);
    }
}
