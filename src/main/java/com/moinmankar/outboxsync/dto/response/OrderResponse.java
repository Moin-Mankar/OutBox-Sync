package com.moinmankar.outboxsync.dto.response;


import com.moinmankar.outboxsync.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        UUID productId,
        Integer quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}