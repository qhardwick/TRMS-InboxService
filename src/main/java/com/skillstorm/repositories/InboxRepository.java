package com.skillstorm.repositories;

import com.skillstorm.entities.Inbox;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface InboxRepository extends ReactiveCassandraRepository<Inbox, String> {

    // Find all entries for a given username:
    @Query("SELECT * FROM inbox WHERE username = ?0")
    Flux<Inbox> findAllById(String username);
}
