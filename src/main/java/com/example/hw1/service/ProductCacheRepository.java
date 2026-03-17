package com.example.hw1.service;

import java.time.Duration;
import java.util.Optional;

import com.example.hw1.cache.CacheValue;
import com.example.hw1.domain.Product;

public interface ProductCacheRepository {

    Optional<CacheValue> get(Long id);

    void put(Long id, Product product, Duration ttl);

    void putNull(Long id, Duration ttl);

    boolean tryLock(Long id);

    void unlock(Long id);

    void evict(Long id);
}
