package com.example.hw1.order.service;

import java.util.List;
import java.util.Optional;

import com.example.hw1.config.KafkaTopicsProperties;
import com.example.hw1.order.domain.Order;
import com.example.hw1.order.domain.OrderStatus;
import com.example.hw1.order.mapper.OrderMapper;
import com.example.hw1.outbox.service.OutboxService;
import com.example.hw1.seckill.messaging.InventoryDeductEvent;
import com.example.hw1.seckill.messaging.OrderCreateEvent;
import com.example.hw1.seckill.messaging.OrderStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final OutboxService outboxService;
    private final KafkaTopicsProperties topics;

    public OrderService(OrderMapper orderMapper, OutboxService outboxService, KafkaTopicsProperties topics) {
        this.orderMapper = orderMapper;
        this.outboxService = outboxService;
        this.topics = topics;
    }

    /**
     * Create a PENDING_INVENTORY order AND stage the inventory-deduct message
     * in the same DB transaction. The outbox publisher picks it up.
     *
     * <p>Idempotent: UNIQUE KEY (user_id, product_id) + INSERT IGNORE means
     * retried deliveries of the same OrderCreateEvent do not create duplicates.
     */
    @Transactional
    public void createPendingOrder(OrderCreateEvent event) {
        Order order = new Order();
        order.setId(Long.parseLong(event.getOrderId()));
        order.setUserId(event.getUserId());
        order.setProductId(event.getProductId());
        order.setQuantity(event.getQuantity());
        order.setAmount(event.getAmount());
        order.setStatus(OrderStatus.PENDING_INVENTORY.name());

        int inserted = orderMapper.insertIgnore(order);
        if (inserted == 0) {
            log.info("order {} already exists, skip (idempotent)", event.getOrderId());
            return;
        }

        InventoryDeductEvent deduct = new InventoryDeductEvent(
                event.getMessageId(), event.getOrderId(), event.getProductId(), event.getQuantity());
        outboxService.stage("order:" + event.getOrderId(), topics.getInventoryDeduct(),
                event.getOrderId(), deduct);
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderMapper.findById(id));
    }

    public List<Order> findByUser(Long userId) {
        return orderMapper.findByUser(userId);
    }

    @Transactional
    public void applyStatus(OrderStatusEvent event) {
        int updated = orderMapper.updateStatus(
                Long.parseLong(event.getOrderId()),
                inferFromStatus(event.getNewStatus()),
                event.getNewStatus());
        if (updated == 0) {
            log.warn("order {} status update to {} did not match expected prior state",
                    event.getOrderId(), event.getNewStatus());
        }
    }

    @Transactional
    public void pay(Long orderId) {
        int updated = orderMapper.updateStatus(orderId,
                OrderStatus.PENDING_PAYMENT.name(), OrderStatus.PAID.name());
        if (updated == 0) {
            throw new IllegalStateException("订单不存在或状态不允许支付");
        }
        OrderStatusEvent evt = new OrderStatusEvent(
                "pay-" + orderId, String.valueOf(orderId), OrderStatus.PAID.name(), "payment-success");
        outboxService.stage("order:" + orderId, topics.getOrderStatus(), String.valueOf(orderId), evt);
    }

    private String inferFromStatus(String newStatus) {
        return switch (OrderStatus.valueOf(newStatus)) {
            case PENDING_PAYMENT -> OrderStatus.PENDING_INVENTORY.name();
            case PAID -> OrderStatus.PENDING_PAYMENT.name();
            case CANCELLED -> OrderStatus.PENDING_INVENTORY.name();
            default -> newStatus;
        };
    }
}
