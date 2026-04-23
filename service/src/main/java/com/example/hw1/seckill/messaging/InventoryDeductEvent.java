package com.example.hw1.seckill.messaging;

public class InventoryDeductEvent {

    private String messageId;
    private String orderId;
    private Long productId;
    private Integer quantity;

    public InventoryDeductEvent() {}

    public InventoryDeductEvent(String messageId, String orderId, Long productId, Integer quantity) {
        this.messageId = messageId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String v) { this.messageId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }
}
