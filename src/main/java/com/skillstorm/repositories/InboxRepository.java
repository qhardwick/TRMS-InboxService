package com.skillstorm.repositories;

import com.skillstorm.entities.Inbox;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxRepository extends ReactiveCassandraRepository<Inbox, String> {
}
