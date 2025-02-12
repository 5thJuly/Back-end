package org.example.technihongo.dto;

import lombok.*;
import org.example.technihongo.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentTransactionDTO {
    private Integer transactionId;
    private String subscriptionPlanName;
    private String paymentMethod;
    private BigDecimal transactionAmount;
    private String currency;
    private TransactionStatus transactionStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}
