package com.skillstorm.utils;

import com.skillstorm.constants.Queues;
import com.skillstorm.dtos.ApprovalRequestDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventBus {

    private final RabbitTemplate rabbitTemplate;

    private final Map<String, FluxSink<ApprovalRequestDto>> subscribers = new ConcurrentHashMap<>();

    @Autowired
    public EventBus(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String username, ApprovalRequestDto approvalRequest) {
        rabbitTemplate.convertAndSend("user".concat(username.toLowerCase()), approvalRequest);
    }

    public Flux<ApprovalRequestDto> subscribe(String username) {
        return Flux.create(sink -> {
            subscribers.put(username.toLowerCase(), sink);
            sink.onDispose(() -> subscribers.remove(username.toLowerCase()));
        }, FluxSink.OverflowStrategy.LATEST);
    }

    @RabbitListener(queues = "approval-request-updates-queue")
    public  void onMessage(ApprovalRequestDto approvalRequest) {
        FluxSink<ApprovalRequestDto> sink = subscribers.get(approvalRequest.getUsername());
        if(sink != null) {
            sink.next(approvalRequest);
        }
    }
}
