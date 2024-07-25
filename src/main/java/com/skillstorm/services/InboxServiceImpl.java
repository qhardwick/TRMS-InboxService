package com.skillstorm.services;

import com.skillstorm.repositories.InboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InboxServiceImpl implements InboxService {

    private final InboxRepository inboxRepository;

    @Autowired
    public InboxServiceImpl(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }
}
