package com.example.hw1.seckill.messaging;

public class OrderStatusEvent {

    private String messageId;
    private String orderId;
    private String newStatus;
    private String reason;

    public OrderStatusEvent() {}

    public OrderStatusEvent(String messageId, String orderId, String newStatus, String reason) {
        this.messageId = messageId;
        this.orderId = orderId;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String v) { this.messageId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String v) { this.newStatus = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
