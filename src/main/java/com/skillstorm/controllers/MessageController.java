package com.skillstorm.controllers;

import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // Mark message as read. May be refactored into a more general 'Edit Message' method, but I believe messages should be largely immutable:
    @PutMapping("/{formId}")
    public Mono<ApprovalRequestDto> markMessageAsViewed(@RequestHeader("username") String username, @PathVariable("formId") UUID formId) {
        return messageService.markMessageAsViewed(username, formId);
    }

    // Delete a message from inbox:
    @DeleteMapping
    public Mono<Void> deleteByUsernameAndFormId(@RequestBody Mono<ApprovalRequestDto> approvalRequestDto) {
        return approvalRequestDto.flatMap(messageService::deleteByUsernameAndFormId);
    }
}
