package com.example.hw1.seckill.dto;

public record SeckillResult(
        boolean accepted,
        String code,
        String message,
        String orderId,
        Long remainingStock) {

    public static SeckillResult accepted(String orderId, long remaining) {
        return new SeckillResult(true, "ACCEPTED",
                "下单请求已受理，订单正在异步创建", orderId, remaining);
    }

    public static SeckillResult duplicate(String existingOrderId) {
        return new SeckillResult(false, "DUPLICATE",
                "您已参与过该商品的秒杀", existingOrderId, null);
    }

    public static SeckillResult soldOut() {
        return new SeckillResult(false, "SOLD_OUT",
                "库存不足，秒杀结束", null, 0L);
    }

    public static SeckillResult notReady() {
        return new SeckillResult(false, "NOT_READY",
                "活动未就绪，请稍后再试", null, null);
    }
}
