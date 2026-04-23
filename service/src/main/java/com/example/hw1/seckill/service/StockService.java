package com.example.hw1.seckill.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Redis-backed stock for seckill.
 * Pre-deduction is performed atomically via Lua so that we never oversell,
 * even under extreme concurrency from multiple app instances.
 */
@Service
public class StockService {

    static final String STOCK_KEY_PREFIX = "seckill:stock:";
    static final String IDEMPOTENCY_KEY_PREFIX = "seckill:idem:";

    // KEYS[1] = stock key; ARGV[1] = delta (positive to reserve)
    // Returns remaining stock or -1 if insufficient, -2 if key missing.
    private static final String DEDUCT_LUA =
            "local s = redis.call('GET', KEYS[1])\n" +
            "if not s then return -2 end\n" +
            "local v = tonumber(s)\n" +
            "local d = tonumber(ARGV[1])\n" +
            "if v < d then return -1 end\n" +
            "return redis.call('DECRBY', KEYS[1], d)";

    private static final String ROLLBACK_LUA =
            "local s = redis.call('GET', KEYS[1])\n" +
            "if not s then return -2 end\n" +
            "return redis.call('INCRBY', KEYS[1], ARGV[1])";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> deductScript;
    private final DefaultRedisScript<Long> rollbackScript;
    private final Duration idempotencyTtl;

    public StockService(
            StringRedisTemplate redis,
            @Value("${app.seckill.idempotency-ttl-minutes:60}") int idempotencyMinutes) {
        this.redis = redis;
        this.deductScript = new DefaultRedisScript<>(DEDUCT_LUA, Long.class);
        this.rollbackScript = new DefaultRedisScript<>(ROLLBACK_LUA, Long.class);
        this.idempotencyTtl = Duration.ofMinutes(idempotencyMinutes);
    }

    public void initStock(long productId, long quantity, Duration ttl) {
        redis.opsForValue().set(stockKey(productId), String.valueOf(quantity), ttl);
    }

    public Long currentStock(long productId) {
        String s = redis.opsForValue().get(stockKey(productId));
        return s == null ? null : Long.valueOf(s);
    }

    /**
     * @return remaining stock on success, -1 if insufficient, -2 if stock not initialized.
     */
    public long deduct(long productId, long quantity) {
        Long r = redis.execute(deductScript, List.of(stockKey(productId)), String.valueOf(quantity));
        return r == null ? -2L : r;
    }

    public long rollback(long productId, long quantity) {
        Long r = redis.execute(rollbackScript, List.of(stockKey(productId)), String.valueOf(quantity));
        return r == null ? -2L : r;
    }

    /**
     * Idempotency gate: one user can only seckill one product once.
     * @return true if this is the first attempt (lock acquired), false if already seckilled.
     */
    public boolean tryAcquireUserSlot(long userId, long productId, String orderId) {
        Boolean ok = redis.opsForValue()
                .setIfAbsent(idemKey(userId, productId), orderId, idempotencyTtl);
        return Boolean.TRUE.equals(ok);
    }

    public void releaseUserSlot(long userId, long productId) {
        redis.delete(idemKey(userId, productId));
    }

    public String existingOrderId(long userId, long productId) {
        return redis.opsForValue().get(idemKey(userId, productId));
    }

    private String stockKey(long productId) { return STOCK_KEY_PREFIX + productId; }
    private String idemKey(long userId, long productId) {
        return IDEMPOTENCY_KEY_PREFIX + userId + ":" + productId;
    }
}
