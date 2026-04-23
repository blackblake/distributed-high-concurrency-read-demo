package com.example.hw1.seckill.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SeckillRequest {

    @NotNull
    @Positive
    private Long userId;

    @NotNull
    @Positive
    private Long productId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
