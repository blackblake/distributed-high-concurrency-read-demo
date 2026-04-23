package com.example.hw1.cache;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static String productDetailKey(Long id) {
        return "product:detail:" + id;
    }

    public static String productLockKey(Long id) {
        return "lock:product:" + id;
    }
}
