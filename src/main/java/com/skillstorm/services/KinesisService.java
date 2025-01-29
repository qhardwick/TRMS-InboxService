package com.skillstorm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.entities.ApprovalRequest;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KinesisService {

    private final KinesisAsyncClient kinesisClient;
    private final String streamName;
    private final String consumerArn;
    private final ObjectMapper mapper;

    // Ensure we're only subscribed to each shard once:
    private final Map<String, CompletableFuture<Void>> shardSubscriptions = new ConcurrentHashMap<>();

    @Autowired
    public KinesisService(KinesisAsyncClient kinesisClient, @Value("${kinesis.stream-name}") String streamName, @Value("${kinesis.consumer-arn}") String consumerArn, ObjectMapper mapper) {
        this.kinesisClient = kinesisClient;
        this.streamName = streamName;
        this.consumerArn = consumerArn;
        this.mapper = mapper;
    }

    // Load data into the kinesis stream for consumption:
    public void publishApprovalRequestToKinesis(ApprovalRequest approvalRequest) {
        try {
            System.out.println("\nPublishing new message to kinesis: " + approvalRequest.toString());
            String jsonData = mapper.writeValueAsString(new ApprovalRequestDto(approvalRequest));

            PutRecordRequest putRequest = PutRecordRequest.builder()
                    .streamName(streamName)
                    .partitionKey(approvalRequest.getUsername())
                    .data(SdkBytes.fromUtf8String(jsonData))
                    .build();

            kinesisClient.putRecord(putRequest);

        } catch ( JsonProcessingException e) {
            System.err.println("Publish message failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void subscribeToKinesisStream(FluxSink<ApprovalRequestDto> sink) {
        ListShardsRequest listShardsRequest = ListShardsRequest.builder().streamName(streamName).build();
        kinesisClient.listShards(listShardsRequest).whenComplete((listShardsResponse, ex) -> {
            if (ex == null) {
                System.out.println("\n\nSubscribing to stream");
                listShardsResponse.shards().forEach(shard -> {
                    System.out.println("\nIdentified shard: " + shard.toString());
                    if (!shardSubscriptions.containsKey(shard.shardId())) {
                        System.out.println("\nAdding shard " + shard.shardId() + " to map");
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
                    System.out.println("\nEvent received: " + event.toString());
                    if (event instanceof SubscribeToShardEvent) {
                        ((SubscribeToShardEvent) event).records().forEach(record -> {
                            try {
                                System.out.println("\nRecord received: " + record.toString());
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
