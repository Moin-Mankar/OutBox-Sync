package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.dto.request.CreateOrderRequest;
import com.moinmankar.outboxsync.dto.response.OrderResponse;
import com.moinmankar.outboxsync.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {

        OrderResponse response =
                orderService.createOrder(authentication.getName(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}