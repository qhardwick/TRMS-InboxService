package com.skillstorm.services;

import com.skillstorm.dtos.ApprovalRequestDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageService {

    // Find all inbox messages. Just for testing:
    Flux<ApprovalRequestDto> findAll();

    // Get all Forms awaiting a User's approval:
    Flux<ApprovalRequestDto> getAllAwaitingApprovalByUsername(String username);

    // Delete by Username and FormId:
    Mono<Void> deleteByUsernameAndFormId(ApprovalRequestDto approvalRequest);
}
