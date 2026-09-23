package com.moinmankar.outboxsync.repository;

import com.moinmankar.outboxsync.entity.OutboxEvent;
import com.moinmankar.outboxsync.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatus(EventStatus status);

    List<OutboxEvent> findByStatusAndLastAttemptedAtBefore(EventStatus status, LocalDateTime cutoff);
}