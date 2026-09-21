package com.moinmankar.outboxsync.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank
        String name,

        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity

) {
}