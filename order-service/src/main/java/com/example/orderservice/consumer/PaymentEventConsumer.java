package com.example.orderservice.consumer;

import com.example.dto.PaymentProcessedEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void consumePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent from Kafka: {}", event);
        try {
            orderService.updateOrderStatus(event.getOrderId(), event.getPaymentStatus());
            log.info("Successfully updated order {} status to match payment status: {}", event.getOrderId(), event.getPaymentStatus());
        } catch (Exception e) {
            log.error("Failed to process PaymentProcessedEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
