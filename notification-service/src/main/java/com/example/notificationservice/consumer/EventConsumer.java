package com.example.notificationservice.consumer;

import com.example.dto.OrderCreatedEvent;
import com.example.dto.PaymentProcessedEvent;
import com.example.dto.UserCreatedEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void consumeUserCreated(UserCreatedEvent event) {
        log.info("Notification Service received UserCreatedEvent: {}", event);
        String message = String.format("Welcome to our platform, %s! Your account is ready.", event.getFullName());
        notificationService.saveNotification(event.getUserId(), null, "EMAIL", message);
        log.info("Email notification simulated and saved for User: {}", event.getUserId());
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void consumeOrderCreated(OrderCreatedEvent event) {
        log.info("Notification Service received OrderCreatedEvent: {}", event);
        String message = String.format("Thank you! Your order %s has been placed successfully for a total amount of $%s.", 
                event.getOrderId(), event.getTotalAmount());
        notificationService.saveNotification(event.getUserId(), event.getOrderId(), "EMAIL", message);
        log.info("Order confirmation notification simulated and saved for Order: {}", event.getOrderId());
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void consumePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Notification Service received PaymentProcessedEvent: {}", event);
        String message;
        if ("SUCCESS".equalsIgnoreCase(event.getPaymentStatus())) {
            message = String.format("Payment SUCCESS for your order %s. Thank you for your purchase!", event.getOrderId());
        } else {
            message = String.format("Payment FAILED for your order %s. Please try again or contact support.", event.getOrderId());
        }
        
        // Note: For payment-events we don't have the user ID in the event itself.
        // In a real microservice, we could either query the order-service via REST or gRPC, 
        // or have the payment-event include the user ID. 
        // For simplicity and decoupling, we will save it under user ID 'UNKNOWN' or a placeholder.
        notificationService.saveNotification("UNKNOWN", event.getOrderId(), "SMS", message);
        log.info("Payment alert notification simulated and saved for Order: {}", event.getOrderId());
    }
}
