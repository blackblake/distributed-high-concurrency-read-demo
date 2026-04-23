package com.example.hw1.cache;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class CachePolicy {

    private final int detailTtlMinutes;
    private final int detailJitterMinutes;
    private final int nullValueTtlMinutes;
    private final int lockTtlSeconds;

    public CachePolicy(int detailTtlMinutes, int detailJitterMinutes, int nullValueTtlMinutes, int lockTtlSeconds) {
        this.detailTtlMinutes = detailTtlMinutes;
        this.detailJitterMinutes = detailJitterMinutes;
        this.nullValueTtlMinutes = nullValueTtlMinutes;
        this.lockTtlSeconds = lockTtlSeconds;
    }

    public Duration detailTtl() {
        int jitter = detailJitterMinutes <= 0 ? 0 : ThreadLocalRandom.current().nextInt(detailJitterMinutes + 1);
        return Duration.ofMinutes(detailTtlMinutes + jitter);
    }

    public Duration nullValueTtl() {
        return Duration.ofMinutes(nullValueTtlMinutes);
    }

    public Duration lockTtl() {
        return Duration.ofSeconds(lockTtlSeconds);
    }
}
