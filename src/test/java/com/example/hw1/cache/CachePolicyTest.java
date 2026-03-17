package com.example.hw1.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CachePolicyTest {

    private final CachePolicy cachePolicy = new CachePolicy(30, 10, 2, 10);

    @Test
    void shouldBuildDetailKeyWithProductId() {
        assertEquals("product:detail:42", CacheKeys.productDetailKey(42L));
    }

    @Test
    void shouldUseShortTtlForNullCache() {
        assertEquals(Duration.ofMinutes(2), cachePolicy.nullValueTtl());
    }

    @Test
    void shouldAddJitterForDetailCache() {
        Duration ttl = cachePolicy.detailTtl();

        assertTrue(ttl.toMinutes() >= 30);
        assertTrue(ttl.toMinutes() <= 40);
    }

    @Test
    void shouldUseConfiguredLockTtl() {
        assertEquals(Duration.ofSeconds(10), cachePolicy.lockTtl());
    }
}
