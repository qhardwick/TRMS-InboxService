package com.skillstorm.services;

import com.skillstorm.dtos.InboxDto;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface MessageService {

    // Find all inbox messages. Just for testing:
    Flux<InboxDto> findAll();

    // Get all Forms awaiting a User's approval:
    Flux<UUID> getAllAwaitingApprovalByUsername(String username);
}
