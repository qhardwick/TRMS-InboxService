package com.skillstorm.controllers;

import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    public Flux<ApprovalRequestDto> findAllMessages() {
        return messageService.findAll();
    }

    // View Forms awaiting the User's approval:
    @GetMapping("/pending-my-approval")
    public Flux<UUID> getAllAwaitingApprovalByUsername(@RequestHeader("username") String username) {
        return messageService.getAllAwaitingApprovalByUsername(username);
    }

    // Delete a message from inbox:
    @DeleteMapping
    public Mono<Void> deleteByUsernameAndFormId(@RequestBody ApprovalRequestDto approvalRequestDto) {
        return messageService.deleteByUsernameAndFormId(approvalRequestDto);
    }
}
