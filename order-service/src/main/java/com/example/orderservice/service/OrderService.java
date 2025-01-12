package com.example.orderservice.service;

import com.example.dto.OrderCreatedEvent;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order placeOrder(Order orderRequest) {
        String orderId = UUID.randomUUID().toString();
        orderRequest.setOrderId(orderId);
        orderRequest.setStatus("PENDING");

        // Compute total amount and assign relationships
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : orderRequest.getItems()) {
            item.setOrderItemId(UUID.randomUUID().toString());
            item.setOrder(orderRequest);
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        orderRequest.setTotalAmount(totalAmount);

        // Save order and its items (cascading enabled)
        Order savedOrder = orderRepository.save(orderRequest);
        log.info("Order placed successfully in database with status PENDING: {}", savedOrder.getOrderId());

        // Prepare Kafka Event DTO
        List<OrderCreatedEvent.OrderItemDto> itemDtos = savedOrder.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderItemDto(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                savedOrder.getOrderId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                itemDtos
        );

        // Serialize Event to JSON for Outbox table
        try {
            String eventJson = objectMapper.writeValueAsString(orderCreatedEvent);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .aggregateType("ORDER")
                    .aggregateId(savedOrder.getOrderId())
                    .eventType("ORDER_CREATED")
                    .payload(eventJson)
                    .processed(false)
                    .build();

            // Save Outbox Event in the SAME local database transaction
            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event recorded for Order: {}. Transaction will commit atomically.", savedOrder.getOrderId());

        } catch (Exception e) {
            log.error("Failed to serialize OrderCreatedEvent for outbox storage: {}", e.getMessage(), e);
            throw new RuntimeException("Error preparing transactional outbox event", e);
        }

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(String orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            order.setStatus("PAID");
            log.info("Order {} status updated to PAID", orderId);
        } else {
            order.setStatus("PAYMENT_FAILED");
            log.warn("Order {} status updated to PAYMENT_FAILED due to failed payment", orderId);
        }

        orderRepository.save(order);
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
