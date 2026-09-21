package com.moinmankar.outboxsync.controller;

import com.moinmankar.outboxsync.dto.response.OutboxEventResponse;
import com.moinmankar.outboxsync.entity.OutboxEvent;
import com.moinmankar.outboxsync.enums.EventStatus;
import com.moinmankar.outboxsync.repository.OutboxEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/outbox")
public class AdminOutboxController {

    private final OutboxEventRepository outboxEventRepository;

    public AdminOutboxController(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<OutboxEventResponse>> getPendingEvents() {

        return ResponseEntity.ok(
                outboxEventRepository
                        .findByStatus(EventStatus.PENDING)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @GetMapping("/failed")
    public ResponseEntity<List<OutboxEventResponse>> getFailedEvents() {

        return ResponseEntity.ok(
                outboxEventRepository
                        .findByStatus(EventStatus.FAILED)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private OutboxEventResponse toResponse(OutboxEvent event) {

        return new OutboxEventResponse(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getLastAttemptedAt()
        );
    }
}