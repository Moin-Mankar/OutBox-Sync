package com.moinmankar.outboxsync.service;

import com.moinmankar.outboxsync.entity.OutboxEvent;
import com.moinmankar.outboxsync.enums.EventStatus;
import com.moinmankar.outboxsync.enums.EventType;
import com.moinmankar.outboxsync.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxWorkerReclaimTest {

    private OutboxEventRepository repository;
    private KafkaProducerService producer;
    private OutboxWorker worker;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        producer = mock(KafkaProducerService.class);
        worker = new OutboxWorker(repository, producer);
    }

    @Test
    void staleProcessingEventIsReclaimedBackToPending() {

        OutboxEvent stale = new OutboxEvent();
        stale.setId(UUID.randomUUID());
        stale.setEventType(EventType.ORDER_CREATED);
        stale.setPayload("{}");
        stale.setStatus(EventStatus.PROCESSING);
        stale.setRetryCount(1);
        stale.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        stale.setLastAttemptedAt(LocalDateTime.now().minusMinutes(5));

        when(repository.findByStatusAndLastAttemptedAtBefore(
                eq(EventStatus.PROCESSING), any(LocalDateTime.class)))
                .thenReturn(List.of(stale));
        when(repository.findByStatus(EventStatus.PENDING))
                .thenReturn(List.of());

        worker.processOutboxEvents();

        assertThat(stale.getStatus()).isEqualTo(EventStatus.PENDING);
        verify(repository).save(stale);
    }

    @Test
    void reclaimedEventIsPublishedOnSameCycle() {

        OutboxEvent reclaimed = new OutboxEvent();
        reclaimed.setId(UUID.randomUUID());
        reclaimed.setEventType(EventType.ORDER_CREATED);
        reclaimed.setPayload("{}");
        reclaimed.setRetryCount(0);
        reclaimed.setLastAttemptedAt(LocalDateTime.now().minusMinutes(5));

        when(repository.findByStatusAndLastAttemptedAtBefore(
                eq(EventStatus.PROCESSING), any(LocalDateTime.class)))
                .thenReturn(List.of(reclaimed));
        when(repository.findByStatus(EventStatus.PENDING))
                .thenReturn(List.of(reclaimed));

        worker.processOutboxEvents();

        verify(producer, times(1)).publish(eq(reclaimed.getId().toString()), eq("{}"));
    }
}
