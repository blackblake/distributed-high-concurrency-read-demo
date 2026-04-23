package com.example.hw1.seckill.messaging;

import com.example.hw1.inventory.service.InventoryService;
import com.example.hw1.order.service.OrderService;
import com.example.hw1.seckill.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class SeckillEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillEventConsumer.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final StockService stockService;
    private final ObjectMapper objectMapper;

    public SeckillEventConsumer(OrderService orderService,
                                InventoryService inventoryService,
                                StockService stockService,
                                ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.stockService = stockService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-create}", groupId = "hw1-order-service")
    public void onOrderCreate(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            OrderCreateEvent event = objectMapper.readValue(record.value(), OrderCreateEvent.class);
            orderService.createPendingOrder(event);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("order-create consume failed key={}", record.key(), ex);
            // Do not ack — Kafka will redeliver.
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.inventory-deduct}", groupId = "hw1-inventory-service")
    public void onInventoryDeduct(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            InventoryDeductEvent event = objectMapper.readValue(record.value(), InventoryDeductEvent.class);
            inventoryService.applyDeduct(event);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("inventory-deduct consume failed key={}", record.key(), ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.order-status}", groupId = "hw1-order-service")
    public void onOrderStatusUpdate(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            OrderStatusEvent event = objectMapper.readValue(record.value(), OrderStatusEvent.class);
            orderService.applyStatus(event);
            // Compensate Redis stock if inventory-service rejected
            if ("CANCELLED".equals(event.getNewStatus())) {
                // Best-effort rollback: recover pre-deducted slot
                try {
                    Long orderId = Long.valueOf(event.getOrderId());
                    var order = orderService.findById(orderId).orElse(null);
                    if (order != null) {
                        stockService.rollback(order.getProductId(), order.getQuantity());
                        stockService.releaseUserSlot(order.getUserId(), order.getProductId());
                    }
                } catch (Exception ignore) { /* best-effort */ }
            }
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("order-status consume failed key={}", record.key(), ex);
        }
    }
}
