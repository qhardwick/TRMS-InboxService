package com.skillstorm.repositories;

import com.skillstorm.entities.ApprovalRequest;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface ApprovalRequestRepository extends ReactiveCassandraRepository<ApprovalRequest, String> {

    // Find all entries for a given username:
    @Query("SELECT * FROM inbox WHERE username = ?0")
    Flux<ApprovalRequest> findAllById(String username);

    // Find all ApprovalRequests whose deadlines have passed:
    Flux<ApprovalRequest> findAllRequestsWithExpiredDeadlines(LocalDateTime deadline);
}
