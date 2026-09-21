package com.moinmankar.outboxsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moinmankar.outboxsync.dto.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "order-processing-group"
    )
    public void consume(String message) {

        try {

            OrderCreatedEvent event =
                    objectMapper.readValue(
                            message,
                            OrderCreatedEvent.class
                    );

            log.info(
                    "[KAFKA] Order event received: orderId={}",
                    event.orderId()
            );

            processOrder(event);

        } catch (Exception e) {

            log.error(
                    "[KAFKA] Failed to process order event",
                    e
            );

            throw new RuntimeException(
                    "Failed to process Kafka event",
                    e
            );
        }
    }

    private void processOrder(OrderCreatedEvent event) {

        log.info(
                "[ORDER PROCESSING] Processing order {}",
                event.orderId()
        );

    }
}