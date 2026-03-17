package com.example.hw1.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDetailResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String description,
        String status,
        String source,
        boolean nullCache,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
