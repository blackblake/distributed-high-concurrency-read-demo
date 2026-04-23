package com.example.hw1.order.mapper;

import java.util.List;

import com.example.hw1.order.domain.Order;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {

    int insertIgnore(Order order);

    Order findById(@Param("id") Long id);

    List<Order> findByUser(@Param("userId") Long userId);

    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);
}
