package com.example.hw1.seckill.messaging;

import java.math.BigDecimal;

public class OrderCreateEvent {

    private String messageId;
    private String orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;

    public OrderCreateEvent() {}

    public OrderCreateEvent(String messageId, String orderId, Long userId, Long productId,
                            Integer quantity, BigDecimal amount) {
        this.messageId = messageId;
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String v) { this.messageId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
}
