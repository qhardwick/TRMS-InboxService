package com.skillstorm.repositories;

import com.datastax.oss.driver.api.core.data.TupleValue;
import com.skillstorm.entities.ApprovalRequest;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends ReactiveCassandraRepository<ApprovalRequest, String> {

    // Find by username and formId:
    @Query("SELECT * FROM approval_request WHERE username = ?0 AND form_id = ?1")
    Mono<ApprovalRequest> findByUsernameAndFormId(String username, UUID formId);

    // Delete by username and formId:
    @Query("DELETE FROM approval_request WHERE username =?0 AND form_id = ?1")
    Mono<Void> deleteByUsernameAndFormId(String username, UUID formId);

    // Find all entries for a given username:
    @Query("SELECT * FROM approval_request WHERE username = ?0")
    Flux<ApprovalRequest> findAllByUsername(String username);

    // Find all ApprovalRequests whose deadlines have passed:
    @Query("SELECT * FROM approval_request WHERE approval_deadline < ?0 ALLOW FILTERING")
    Flux<ApprovalRequest> findAllRequestsWithExpiredDeadlines(LocalDateTime deadline);
}
