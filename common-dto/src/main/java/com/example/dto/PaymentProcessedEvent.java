package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent implements Serializable {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String paymentStatus; // SUCCESS, FAILED
    private String transactionId;
}
