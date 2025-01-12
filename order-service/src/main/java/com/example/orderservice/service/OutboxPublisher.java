package com.example.orderservice.service;

import com.example.dto.OrderCreatedEvent;
import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Scheduled(fixedDelay = 5000) // Poll database every 5 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox Poller: Found {} pending outbox event(s) to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                if ("ORDER_CREATED".equals(event.getEventType())) {
                    // Deserialize payload back to DTO
                    OrderCreatedEvent orderCreatedEvent = objectMapper.readValue(
                            event.getPayload(), 
                            OrderCreatedEvent.class
                    );

                    // Publish to Kafka topic
                    kafkaTemplate.send(ORDER_EVENTS_TOPIC, event.getAggregateId(), orderCreatedEvent).get();
                    
                    log.info("Outbox Poller: Successfully published event {} to topic {}", event.getEventId(), ORDER_EVENTS_TOPIC);
                } else {
                    log.warn("Outbox Poller: Unknown event type: {}", event.getEventType());
                }

                // Update outbox event status as processed
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

            } catch (Exception e) {
                log.error("Outbox Poller: Failed to publish event {}: {}. Will retry next run.", 
                        event.getEventId(), e.getMessage());
                // Break to preserve ordering and avoid spamming on recurring errors
                break;
            }
        }
    }
}
