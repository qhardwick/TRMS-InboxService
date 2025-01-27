package com.skillstorm.controllers;

import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.services.KinesisService;
import com.skillstorm.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final KinesisService kinesisService;

    @Autowired
    public MessageController(MessageService messageService, KinesisService kinesisService) {
        this.messageService = messageService;
        this.kinesisService = kinesisService;
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

    // Delete a message from inbox:
    @DeleteMapping
    public Mono<Void> deleteByUsernameAndFormId(@RequestBody Mono<ApprovalRequestDto> approvalRequestDto) {
        return approvalRequestDto.flatMap(messageService::deleteByUsernameAndFormId);
    }
}
