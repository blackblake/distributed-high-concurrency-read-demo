package com.example.hw1.outbox.service;

import com.example.hw1.outbox.domain.OutboxMessage;
import com.example.hw1.outbox.mapper.OutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * Writes outbox rows inside the caller's transaction. The OutboxPublisher
 * picks them up and publishes to Kafka — guarantees at-least-once delivery
 * aligned with the DB commit (transactional outbox pattern).
 */
@Service
public class OutboxService {

    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxMapper outboxMapper, ObjectMapper objectMapper) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
    }

    public void stage(String aggregate, String topic, String msgKey, Object payload) {
        OutboxMessage m = new OutboxMessage();
        m.setAggregate(aggregate);
        m.setTopic(topic);
        m.setMsgKey(msgKey);
        try {
            m.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
        outboxMapper.insert(m);
    }
}
