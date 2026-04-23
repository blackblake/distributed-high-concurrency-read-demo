package com.example.hw1.outbox.domain;

import java.time.LocalDateTime;

public class OutboxMessage {

    private Long id;
    private String aggregate;
    private String topic;
    private String msgKey;
    private String payload;
    private String status;
    private Integer retries;
    private LocalDateTime createTime;
    private LocalDateTime sendTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAggregate() { return aggregate; }
    public void setAggregate(String aggregate) { this.aggregate = aggregate; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getMsgKey() { return msgKey; }
    public void setMsgKey(String msgKey) { this.msgKey = msgKey; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetries() { return retries; }
    public void setRetries(Integer retries) { this.retries = retries; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getSendTime() { return sendTime; }
    public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
}
