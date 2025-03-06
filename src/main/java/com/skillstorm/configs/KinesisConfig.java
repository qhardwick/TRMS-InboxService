package com.skillstorm.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

@Configuration
public class KinesisConfig {

    @Bean
    public KinesisAsyncClient kinesisAsyncClient() {
        return KinesisAsyncClient.builder()
                .region(Region.US_EAST_2)
                .build();
    }

}
