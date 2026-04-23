package com.example.hw1.inventory.service;

import com.example.hw1.config.KafkaTopicsProperties;
import com.example.hw1.inventory.domain.Inventory;
import com.example.hw1.inventory.mapper.InventoryMapper;
import com.example.hw1.order.domain.OrderStatus;
import com.example.hw1.outbox.service.OutboxService;
import com.example.hw1.seckill.messaging.InventoryDeductEvent;
import com.example.hw1.seckill.messaging.OrderStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryMapper inventoryMapper;
    private final OutboxService outboxService;
    private final KafkaTopicsProperties topics;

    public InventoryService(InventoryMapper inventoryMapper,
                            OutboxService outboxService,
                            KafkaTopicsProperties topics) {
        this.inventoryMapper = inventoryMapper;
        this.outboxService = outboxService;
        this.topics = topics;
    }

    public Inventory get(Long productId) {
        return inventoryMapper.findByProduct(productId);
    }

    /**
     * Deduct inventory for an order and stage the status-update message in
     * the same transaction.
     *
     * <p>Idempotency: `inventory_log` has a PK on message_id and we use
     * INSERT IGNORE. If the same message is redelivered, the log insert
     * returns 0 rows and we short-circuit.
     */
    @Transactional
    public void applyDeduct(InventoryDeductEvent event) {
        int logged = inventoryMapper.insertLog(event.getMessageId(),
                event.getProductId(), event.getQuantity());
        if (logged == 0) {
            log.info("inventory event {} already processed, skip", event.getMessageId());
            return;
        }

        int updated = inventoryMapper.deduct(event.getProductId(), event.getQuantity());
        String newStatus = (updated > 0)
                ? OrderStatus.PENDING_PAYMENT.name()
                : OrderStatus.CANCELLED.name();
        String reason = (updated > 0) ? "inventory-reserved" : "inventory-insufficient";

        OrderStatusEvent status = new OrderStatusEvent(
                "stat-" + event.getMessageId(), event.getOrderId(), newStatus, reason);
        outboxService.stage("inventory:" + event.getProductId(),
                topics.getOrderStatus(), event.getOrderId(), status);
    }
}
