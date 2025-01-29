package com.skillstorm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.dtos.ApprovalRequestDto;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KinesisService {

    private final Flux<ApprovalRequestDto> updates;
    private final KinesisAsyncClient kinesisClient;
    private final String streamName;
    private final String streamArn;
    private final String consumerName;
    private String consumerArn;

    private final ObjectMapper mapper;

    // Ensure we're only subscribed to each shard once:
    private final Map<String, CompletableFuture<Void>> shardSubscriptions = new ConcurrentHashMap<>();

    @Autowired
    public KinesisService(KinesisAsyncClient kinesisClient, @Value("${kinesis.stream-name}") String streamName, @Value("${kinesis.stream-arn}") String streamArn,
                          @Value("${kinesis.consumer-name}") String consumerName, @Value("${kinesis.consumer-arn}") String consumerArn, ObjectMapper mapper) {
        this.kinesisClient = kinesisClient;
        this.streamName = streamName;
        this.streamArn = streamArn;
        this.consumerName = consumerName;
        this.consumerArn = consumerArn;
        this.updates = Flux.create(this::subscribeToKinesisStream);

        this.mapper = mapper;
    }

    private void subscribeToKinesisStream(FluxSink<ApprovalRequestDto> sink) {
        ListShardsRequest listShardsRequest = ListShardsRequest.builder().streamName(streamName).build();
        kinesisClient.listShards(listShardsRequest).whenComplete((listShardsResponse, ex) -> {
            if (ex == null) {
                System.out.println("\n\nSubscribing to stream");
                listShardsResponse.shards().forEach(shard -> {
                    if (!shardSubscriptions.containsKey(shard.shardId())) {
                        shardSubscriptions.put(shard.shardId(), subscribeToShard(shard, sink));
                    }
                });
            } else {
                System.err.println("\n\nError listing shards: " + ex.getMessage());
                sink.error(ex);
            }
        });
    }

    private CompletableFuture<Void> subscribeToShard(Shard shard, FluxSink<ApprovalRequestDto> sink) {
        System.out.println("\nSubscribing to shard");
        SubscribeToShardRequest request = SubscribeToShardRequest.builder()
                .consumerARN(consumerArn)
                .shardId(shard.shardId())
                .startingPosition(s -> s.type(ShardIteratorType.LATEST))
                .build();

        System.out.println("\nCreating response handler");
        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler.builder()
                .onError(sink::error)
                .subscriber(event -> {
                    if (event instanceof SubscribeToShardEvent) {
                        ((SubscribeToShardEvent) event).records().forEach(record -> {
                            try {
                                String jsonData = new String(record.data().asByteArray(), StandardCharsets.UTF_8);
                                ApprovalRequestDto dto = mapper.readValue(jsonData, ApprovalRequestDto.class);
                                sink.next(dto);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                })
                .build();

        return kinesisClient.subscribeToShard(request, responseHandler);
    }

    public Flux<ApprovalRequestDto> getUpdatesForUser(String username) {
        System.out.println("\nKinesis service is getting updates...");
        return Flux.create(this::subscribeToKinesisStream)
                .filter(approvalRequestDto -> username.equalsIgnoreCase(approvalRequestDto.getUsername()));
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("\n\nShutting down...");
        kinesisClient.close();
    }
}
