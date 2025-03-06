package com.skillstorm.repositories;

import com.skillstorm.entities.VerificationRequest;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRequestRepository extends ReactiveCassandraRepository<VerificationRequest, String> {
}
