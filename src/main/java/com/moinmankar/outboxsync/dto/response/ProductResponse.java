package com.moinmankar.outboxsync.dto.response;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
