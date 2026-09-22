package com.moinmankar.outboxsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moinmankar.outboxsync.dto.event.OrderCreatedEvent;
import com.moinmankar.outboxsync.entity.ProcessedEvent;
import com.moinmankar.outboxsync.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    public OrderEventConsumer(
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository
    ) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "order-processing-group"
    )
    @Transactional
    public void consume(String message) {

        try {

            OrderCreatedEvent event =
                    objectMapper.readValue(
                            message,
                            OrderCreatedEvent.class
                    );

            if (processedEventRepository.existsByEventId(event.eventId())) {

                log.info(
                        "[KAFKA] Duplicate event ignored: eventId={}",
                        event.eventId()
                );

                return;
            }

            processOrder(event);

            ProcessedEvent processedEvent = new ProcessedEvent();

            processedEvent.setEventId(event.eventId());
            processedEvent.setProcessedAt(LocalDateTime.now());

            processedEventRepository.save(processedEvent);

            log.info(
                    "[KAFKA] Event processed successfully: eventId={}",
                    event.eventId()
            );

        } catch (Exception e) {

            log.error(
                    "[KAFKA] Failed to process event",
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
        // Actual business processing will be added here.

    }


}