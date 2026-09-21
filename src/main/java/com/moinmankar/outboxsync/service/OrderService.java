package com.moinmankar.outboxsync.service;


import com.moinmankar.outboxsync.dto.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moinmankar.outboxsync.dto.request.CreateOrderRequest;
import com.moinmankar.outboxsync.dto.response.OrderResponse;
import com.moinmankar.outboxsync.entity.Product;
import com.moinmankar.outboxsync.entity.User;
import com.moinmankar.outboxsync.enums.EventType;
import com.moinmankar.outboxsync.entity.Order;
import com.moinmankar.outboxsync.entity.OutboxEvent;
import com.moinmankar.outboxsync.enums.OrderStatus;
import com.moinmankar.outboxsync.repository.OrderRepository;
import com.moinmankar.outboxsync.repository.OutboxEventRepository;

import com.moinmankar.outboxsync.repository.ProductRepository;
import com.moinmankar.outboxsync.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;


    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ProductRepository productRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(
            String email,
            CreateOrderRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product= productRepository.findById(request.productId()).orElseThrow(()->
                new RuntimeException("Product not Found"));

        if(!product.isActive()){
            throw  new RuntimeException(" Product is not Active");
        }

        if(request.quantity() ==null || request.quantity()<=0){
            throw new RuntimeException("Quantity cannot be Zero");
        }

        if(product.getStockQuantity() < request.quantity()){
            throw new RuntimeException("Insufficient stock");
        }

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        Order order = new Order();

        order.setUser(user);
        order.setProduct(product);
        order.setQuantity(request.quantity());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CREATED);

        product.setStockQuantity(
                product.getStockQuantity() - request.quantity()
        );

        Product savedProduct = productRepository.save(product);

        Order savedOrder = orderRepository.save(order);

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setEventType(EventType.ORDER_CREATED);

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                user.getId(),
                product.getId(),
                request.quantity(),
                totalAmount
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxEvent.setPayload(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create event payload", e);
        }

        outboxEventRepository.save(outboxEvent);

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getUser().getId(),
                savedOrder.getProduct().getId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt(),
                savedOrder.getUpdatedAt()
        );
    }


}
