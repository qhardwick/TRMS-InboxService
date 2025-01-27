package com.skillstorm.controllers;

import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.services.KinesisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/kinesis")
public class KinesisController {

    private final KinesisService kinesisService;

    @Autowired
    public KinesisController(KinesisService kinesisService) {
        this.kinesisService = kinesisService;
    }

    @GetMapping(value = "/pending", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ApprovalRequestDto> pending(@RequestHeader("username") String username) {
        return kinesisService.getUpdatesForUser(username);
    }
}
