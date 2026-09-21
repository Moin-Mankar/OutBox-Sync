package com.moinmankar.outboxsync.dto.response;


import com.moinmankar.outboxsync.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt
) {
}