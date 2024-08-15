package com.skillstorm.services;

import reactor.core.publisher.Flux;

import java.util.UUID;

public interface InboxService {

    // Get all Forms awaiting a User's approval:
    Flux<UUID> getAllAwaitingApprovalByUsername(String username);
}
