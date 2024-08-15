package com.skillstorm.constants;

public enum Queues {

    // From Form-Service:
    INBOX("inbox-queue");

    private final String queue;

    Queues(String queue) {
        this.queue = queue;
    }

    @Override
    public String toString() {
        return queue;
    }
}
