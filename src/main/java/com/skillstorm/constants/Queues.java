package com.skillstorm.constants;

public enum Queues {

    // From Form-Service:
    APPROVAL_REQUEST("approval-request-queue"),

    // To Form-Service:
    AUTO_APPROVAL("automatic-approval-queue");

    private final String queue;

    Queues(String queue) {
        this.queue = queue;
    }

    @Override
    public String toString() {
        return queue;
    }
}
