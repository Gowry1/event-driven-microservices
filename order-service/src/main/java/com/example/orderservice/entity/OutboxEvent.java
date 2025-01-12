package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType; // e.g. "ORDER"

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId; // e.g. orderId

    @Column(name = "event_type", nullable = false)
    private String eventType; // e.g. "ORDER_CREATED"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // Serialized JSON string of the Event DTO

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
