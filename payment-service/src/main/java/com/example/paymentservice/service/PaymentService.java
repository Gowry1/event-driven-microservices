package com.example.paymentservice.service;

import com.example.dto.OrderCreatedEvent;
import com.example.dto.PaymentProcessedEvent;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private final Random random = new Random();

    @Transactional
    public Payment processPayment(OrderCreatedEvent orderEvent) {
        log.info("Processing payment for Order: {}, Amount: {}", orderEvent.getOrderId(), orderEvent.getTotalAmount());

        // Check if payment already exists to prevent duplicate processing (Idempotency)
        if (paymentRepository.findByOrderId(orderEvent.getOrderId()).isPresent()) {
            log.warn("Payment already processed for Order: {}. Skipping.", orderEvent.getOrderId());
            return paymentRepository.findByOrderId(orderEvent.getOrderId()).get();
        }

        String paymentId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();

        // Simulate payment gateway call (90% success rate)
        boolean isSuccess = random.nextInt(10) > 0;
        String status = isSuccess ? "SUCCESS" : "FAILED";

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(orderEvent.getOrderId())
                .amount(orderEvent.getTotalAmount())
                .paymentMethod("CREDIT_CARD")
                .paymentStatus(status)
                .transactionId(transactionId)
                .paidAt(isSuccess ? LocalDateTime.now() : null)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created in database with status {}: {}", status, savedPayment.getPaymentId());

        // Prepare Kafka Event
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                savedPayment.getPaymentId(),
                savedPayment.getOrderId(),
                savedPayment.getAmount(),
                savedPayment.getPaymentStatus(),
                savedPayment.getTransactionId()
        );

        // Publish to Kafka
        try {
            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, savedPayment.getOrderId(), event);
            log.info("Successfully published PaymentProcessedEvent to topic {}: {}", PAYMENT_EVENTS_TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentProcessedEvent to Kafka: {}", e.getMessage(), e);
        }

        return savedPayment;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order id: " + orderId));
    }
}
