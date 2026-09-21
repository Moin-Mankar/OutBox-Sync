package com.moinmankar.outboxsync.dto.event;


import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        UUID productId,
        Integer quantity,
        BigDecimal totalAmount
) {
}