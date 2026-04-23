package com.example.hw1.outbox.mapper;

import java.util.List;

import com.example.hw1.outbox.domain.OutboxMessage;
import org.apache.ibatis.annotations.Param;

public interface OutboxMapper {

    void insert(OutboxMessage message);

    List<OutboxMessage> selectPending(@Param("limit") int limit);

    int markSent(@Param("id") Long id);

    int markFailed(@Param("id") Long id);
}
