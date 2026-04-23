package com.example.hw1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsProperties {

    private String orderCreate = "seckill.order.create";
    private String inventoryDeduct = "inventory.deduct";
    private String orderStatus = "order.status.update";

    public String getOrderCreate() { return orderCreate; }
    public void setOrderCreate(String v) { this.orderCreate = v; }
    public String getInventoryDeduct() { return inventoryDeduct; }
    public void setInventoryDeduct(String v) { this.inventoryDeduct = v; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String v) { this.orderStatus = v; }
}
