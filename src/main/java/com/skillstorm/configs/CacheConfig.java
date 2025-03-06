package com.skillstorm.configs;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.skillstorm.dtos.ApprovalRequestDto;
import com.skillstorm.services.MessageService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("approvalRequests");

        // Set amount of memory initially reserved for cache. We'll be retrieving data from the cache manually rather than
        // using @Cacheable due to the lack of compatibility with Flux, so no need for cacheManager.setAsyncCacheMode(true);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .initialCapacity(10)
                        .maximumSize(1000));
        cacheManager.setAsyncCacheMode(true);
        return cacheManager;
    }

    @Bean
    public AsyncLoadingCache<String, ApprovalRequestDto> approvalRequestsCache(MessageService messageService) {
        return Caffeine.newBuilder()
                .executor(Executors.newFixedThreadPool(1))
                .buildAsync((key, executor) -> loadApprovalRequest(key, messageService));
    }

    private CompletableFuture<ApprovalRequestDto> loadApprovalRequest(String compoundKey, MessageService messageService) {
        // Assuming compoundKey format is username-messageId
        String[] parts = compoundKey.split("-");
        String username = parts[0];
        UUID messageId = UUID.fromString(parts[1]);

        return CompletableFuture.supplyAsync(() -> messageService.getMessageByUsernameAndFormId(username, messageId)
                        .toFuture())
                .thenCompose(Function.identity());
    }
}
