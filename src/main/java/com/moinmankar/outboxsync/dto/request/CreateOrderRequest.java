package com.moinmankar.outboxsync.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.util.UUID;


public record CreateOrderRequest(

        @NotNull
        UUID productId,

        @NotNull
        @Positive
        Integer quantity
) {
}