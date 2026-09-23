package com.moinmankar.outboxsync.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceTokenRequest(
        @NotBlank String token
) {}
