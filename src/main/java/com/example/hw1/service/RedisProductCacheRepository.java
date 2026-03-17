package com.example.hw1.service;

import java.time.Duration;
import java.util.Optional;

import com.example.hw1.cache.CacheKeys;
import com.example.hw1.cache.CachePolicy;
import com.example.hw1.cache.CacheValue;
import com.example.hw1.domain.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisProductCacheRepository implements ProductCacheRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CachePolicy cachePolicy;

    public RedisProductCacheRepository(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            CachePolicy cachePolicy) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cachePolicy = cachePolicy;
    }

    @Override
    public Optional<CacheValue> get(Long id) {
        try {
            String json = stringRedisTemplate.opsForValue().get(CacheKeys.productDetailKey(id));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CacheValue.class));
        } catch (JsonProcessingException | DataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void put(Long id, Product product, Duration ttl) {
        write(CacheKeys.productDetailKey(id), CacheValue.of(product), ttl);
    }

    @Override
    public void putNull(Long id, Duration ttl) {
        write(CacheKeys.productDetailKey(id), CacheValue.nullMarker(), ttl);
    }

    @Override
    public boolean tryLock(Long id) {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(CacheKeys.productLockKey(id), "1", cachePolicy.lockTtl());
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void unlock(Long id) {
        stringRedisTemplate.delete(CacheKeys.productLockKey(id));
    }

    @Override
    public void evict(Long id) {
        stringRedisTemplate.delete(CacheKeys.productDetailKey(id));
    }

    private void write(String key, CacheValue value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize cache value", ex);
        }
    }
}
