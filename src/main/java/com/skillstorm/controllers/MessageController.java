package com.skillstorm.controllers;

import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    // Get all approval requests in a user's inbox. Used for setting the initial state and/or displaying all messages rather than just the new ones:
    @GetMapping("/approval-requests")
    public Flux<ApprovalRequestDto> getApprovalRequestsByUsername(@RequestHeader("username") String username) {
        return messageService.getApprovalRequestsByUsername(username.toLowerCase());
    }

    // SSE emitter to send notification as new messages arrive:
    @GetMapping(value = "/pending", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ApprovalRequestDto>> pending(@RequestParam("username") String username) {
        // We need to tell our program to keep the connection open or it will close it immediately after returning the service method:
        return ResponseEntity.ok()
                        .header(HttpHeaders.CONNECTION, "keep-alive")
                        .body(messageService.getApprovalRequestUpdates(username));
    }

    // Delete a message from inbox:
    @DeleteMapping
    public Mono<Void> deleteByUsernameAndFormId(@RequestBody Mono<ApprovalRequestDto> approvalRequestDto) {
        return approvalRequestDto.flatMap(messageService::deleteByUsernameAndFormId);
    }
}
