package com.example.hw1.seckill.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.hw1.common.SnowflakeIdGenerator;
import com.example.hw1.config.KafkaTopicsProperties;
import com.example.hw1.domain.Product;
import com.example.hw1.seckill.dto.SeckillRequest;
import com.example.hw1.seckill.dto.SeckillResult;
import com.example.hw1.seckill.messaging.OrderCreateEvent;
import com.example.hw1.service.ProductPersistenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final StockService stockService;
    private final SnowflakeIdGenerator idGenerator;
    private final ProductPersistenceService productPersistenceService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper;

    public SeckillService(StockService stockService,
                          SnowflakeIdGenerator idGenerator,
                          ProductPersistenceService productPersistenceService,
                          KafkaTemplate<String, String> kafkaTemplate,
                          KafkaTopicsProperties topics,
                          ObjectMapper objectMapper) {
        this.stockService = stockService;
        this.idGenerator = idGenerator;
        this.productPersistenceService = productPersistenceService;
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
        this.objectMapper = objectMapper;
    }

    @SentinelResource(value = "seckill", blockHandler = "onBlocked", fallback = "onFallback")
    public SeckillResult seckill(SeckillRequest request) {
        long userId = request.getUserId();
        long productId = request.getProductId();

        Long remaining = stockService.currentStock(productId);
        if (remaining == null) {
            return SeckillResult.notReady();
        }

        String orderId = String.valueOf(idGenerator.nextId());
        if (!stockService.tryAcquireUserSlot(userId, productId, orderId)) {
            return SeckillResult.duplicate(stockService.existingOrderId(userId, productId));
        }

        long afterDeduct = stockService.deduct(productId, 1);
        if (afterDeduct < 0) {
            stockService.releaseUserSlot(userId, productId);
            return SeckillResult.soldOut();
        }

        BigDecimal amount = resolvePrice(productId);
        OrderCreateEvent event = new OrderCreateEvent(
                UUID.randomUUID().toString(), orderId, userId, productId, 1, amount);

        try {
            kafkaTemplate.send(topics.getOrderCreate(), orderId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            // This is a programming error — but to be safe, compensate Redis.
            stockService.rollback(productId, 1);
            stockService.releaseUserSlot(userId, productId);
            throw new IllegalStateException("serialize order-create event failed", ex);
        }

        return SeckillResult.accepted(orderId, afterDeduct);
    }

    public SeckillResult onBlocked(SeckillRequest req,
                                   com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("seckill blocked by sentinel: user={} product={}",
                req.getUserId(), req.getProductId());
        return new SeckillResult(false, "RATE_LIMITED",
                "系统繁忙，请稍后再试", null, null);
    }

    public SeckillResult onFallback(SeckillRequest req, Throwable t) {
        log.warn("seckill fallback: user={} product={} reason={}",
                req.getUserId(), req.getProductId(), t.getMessage());
        return new SeckillResult(false, "FALLBACK",
                "服务降级：请稍后再试", null, null);
    }

    private BigDecimal resolvePrice(long productId) {
        Optional<Product> p = productPersistenceService.findByIdForRead(productId);
        return p.map(Product::getPrice).orElse(new BigDecimal("0.00"));
    }
}
