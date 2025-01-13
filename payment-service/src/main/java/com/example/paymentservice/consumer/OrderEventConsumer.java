package com.example.paymentservice.consumer;

import com.example.dto.OrderCreatedEvent;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-events", groupId = "payment-service-group")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent from Kafka: {}", event);
        try {
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Failed to process payment for order event {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
