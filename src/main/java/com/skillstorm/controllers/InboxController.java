package com.skillstorm.controllers;

import com.skillstorm.services.InboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/inboxes")
public class InboxController {

    private final InboxService inboxService;

    @Autowired
    public InboxController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    // Test endpoint:
    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello Inbox Service");
    }

    // View Forms awaiting the User's approval:
    @GetMapping("/pending-my-approval")
    public Flux<UUID> getAllAwaitingApprovalByUsername(@RequestHeader("username") String username) {
        return inboxService.getAllAwaitingApprovalByUsername(username);
    }
}
