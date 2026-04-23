package com.example.hw1.outbox.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.hw1.outbox.domain.OutboxMessage;
import com.example.hw1.outbox.mapper.OutboxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxMapper outboxMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxPublisher(
            OutboxMapper outboxMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.batch-size:50}") int batchSize) {
        this.outboxMapper = outboxMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishPending() {
        List<OutboxMessage> batch = outboxMapper.selectPending(batchSize);
        for (OutboxMessage m : batch) {
            try {
                kafkaTemplate.send(m.getTopic(), m.getMsgKey(), m.getPayload())
                        .get(5, TimeUnit.SECONDS);
                outboxMapper.markSent(m.getId());
            } catch (Exception ex) {
                log.warn("outbox publish failed id={} topic={}", m.getId(), m.getTopic(), ex);
                outboxMapper.markFailed(m.getId());
            }
        }
    }
}
