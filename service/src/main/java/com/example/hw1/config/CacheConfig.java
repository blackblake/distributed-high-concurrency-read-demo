package com.example.hw1.config;

import com.example.hw1.cache.CachePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CachePolicy cachePolicy(
            @Value("${app.cache.product-detail-ttl-minutes}") int productDetailTtlMinutes,
            @Value("${app.cache.product-detail-jitter-minutes}") int productDetailJitterMinutes,
            @Value("${app.cache.null-value-ttl-minutes}") int nullValueTtlMinutes,
            @Value("${app.cache.lock-ttl-seconds}") int lockTtlSeconds) {
        return new CachePolicy(productDetailTtlMinutes, productDetailJitterMinutes, nullValueTtlMinutes, lockTtlSeconds);
    }
}
