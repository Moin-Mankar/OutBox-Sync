package com.moinmankar.outboxsync.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        UUID userId,
        String action,
        String entityType,
        UUID entityId,
        LocalDateTime createdAt
) {}
