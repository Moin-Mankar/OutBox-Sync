package com.moinmankar.outboxsync.service;

import com.moinmankar.outboxsync.entity.OutboxEvent;
import com.moinmankar.outboxsync.enums.EventStatus;
import com.moinmankar.outboxsync.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxWorker {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxWorker.class);

    private static final int MAX_RETRY = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaProducerService kafkaProducerService;

    public OutboxWorker(
            OutboxEventRepository outboxEventRepository,
            KafkaProducerService kafkaProducerService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.findByStatus(EventStatus.PENDING);

        log.info("[OUTBOX] Found {} pending events", events.size());

        for (OutboxEvent event : events) {

            if (event.getRetryCount() >= MAX_RETRY) {
                event.setStatus(EventStatus.FAILED);
                outboxEventRepository.save(event);

                log.error(
                        "[OUTBOX] Event {} reached max retries",
                        event.getId()
                );

                continue;
            }

            event.setStatus(EventStatus.PROCESSING);
            event.setLastAttemptedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            try {

                kafkaProducerService
                        .publish(
                                event.getId().toString(),
                                event.getPayload()
                        )
                        .get(10, TimeUnit.SECONDS);

                event.setStatus(EventStatus.SENT);

                log.info(
                        "[OUTBOX] Event {} published to Kafka",
                        event.getId()
                );

            } catch (Exception e) {

                event.setRetryCount(
                        event.getRetryCount() + 1
                );

                event.setStatus(EventStatus.PENDING);

                log.error(
                        "[OUTBOX] Failed to publish event {}. Retry {}",
                        event.getId(),
                        event.getRetryCount(),
                        e
                );
            }

            outboxEventRepository.save(event);
        }
    }
}