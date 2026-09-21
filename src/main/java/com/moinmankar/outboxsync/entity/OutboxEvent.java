package com.moinmankar.outboxsync.entity;

import com.moinmankar.outboxsync.enums.EventStatus;
import com.moinmankar.outboxsync.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastAttemptedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = EventStatus.PENDING;
        }

        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }
}