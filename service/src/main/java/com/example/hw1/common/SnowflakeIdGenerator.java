package com.example.hw1.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Classic Twitter Snowflake — 64 bits:
 *   1 sign | 41 timestamp-ms | 5 datacenterId | 5 workerId | 12 sequence
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH_MILLIS = 1_700_000_000_000L;
    private static final long WORKER_BITS = 5L;
    private static final long DC_BITS = 5L;
    private static final long SEQ_BITS = 12L;

    private static final long MAX_WORKER = ~(-1L << WORKER_BITS);
    private static final long MAX_DC = ~(-1L << DC_BITS);
    private static final long MAX_SEQ = ~(-1L << SEQ_BITS);

    private static final long WORKER_SHIFT = SEQ_BITS;
    private static final long DC_SHIFT = SEQ_BITS + WORKER_BITS;
    private static final long TS_SHIFT = SEQ_BITS + WORKER_BITS + DC_BITS;

    private final long workerId;
    private final long datacenterId;
    private long lastTs = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(
            @Value("${app.snowflake.worker-id:1}") long workerId,
            @Value("${app.snowflake.datacenter-id:1}") long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER) {
            throw new IllegalArgumentException("workerId out of range 0.." + MAX_WORKER);
        }
        if (datacenterId < 0 || datacenterId > MAX_DC) {
            throw new IllegalArgumentException("datacenterId out of range 0.." + MAX_DC);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) {
            // Clock moved backwards — spin until it catches up.
            ts = waitUntil(lastTs);
        }
        if (ts == lastTs) {
            sequence = (sequence + 1) & MAX_SEQ;
            if (sequence == 0) {
                ts = waitUntil(lastTs + 1);
            }
        } else {
            sequence = 0L;
        }
        lastTs = ts;
        return ((ts - EPOCH_MILLIS) << TS_SHIFT)
                | (datacenterId << DC_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long waitUntil(long target) {
        long ts = System.currentTimeMillis();
        while (ts < target) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}
