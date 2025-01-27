package com.skillstorm.services;

import com.skillstorm.dtos.ApprovalRequestDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MessageService {

    // Find all inbox messages. Just for testing:
    Flux<ApprovalRequestDto> findAll();

    // Get single message by username and form id:
    Mono<ApprovalRequestDto> getMessageByUsernameAndFormId(String username, UUID formId);

    // Get all Forms awaiting a User's approval:
    Flux<ApprovalRequestDto> getAllAwaitingApprovalByUsername(String username);

    // Delete by Username and FormId:
    Mono<Void> deleteByUsernameAndFormId(ApprovalRequestDto approvalRequest);
}
