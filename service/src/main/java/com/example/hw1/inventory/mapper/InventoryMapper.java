package com.example.hw1.inventory.mapper;

import com.example.hw1.inventory.domain.Inventory;
import org.apache.ibatis.annotations.Param;

public interface InventoryMapper {

    Inventory findByProduct(@Param("productId") Long productId);

    /** Conditional deduct — fails (returns 0) if not enough stock. */
    int deduct(@Param("productId") Long productId, @Param("delta") Integer delta);

    int rollback(@Param("productId") Long productId, @Param("delta") Integer delta);

    int insertLog(@Param("messageId") String messageId,
                  @Param("productId") Long productId,
                  @Param("delta") Integer delta);
}
