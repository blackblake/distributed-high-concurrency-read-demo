package com.example.hw1.cache;

import com.example.hw1.domain.Product;

public record CacheValue(boolean nullCache, Product product) {

    public static CacheValue nullMarker() {
        return new CacheValue(true, null);
    }

    public static CacheValue of(Product product) {
        return new CacheValue(false, product);
    }
}
