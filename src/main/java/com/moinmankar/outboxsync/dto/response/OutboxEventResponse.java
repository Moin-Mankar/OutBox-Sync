package com.moinmankar.outboxsync.dto.response;

import com.moinmankar.outboxsync.enums.EventStatus;
import com.moinmankar.outboxsync.enums.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record OutboxEventResponse(
        UUID id,
        EventType eventType,
        EventStatus status,
        Integer retryCount,
        LocalDateTime createdAt,
        LocalDateTime lastAttemptedAt
) {
}
