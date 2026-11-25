package com.paymentflow.payment_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        String idempotencyKey,
        String status,
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        LocalDateTime createdAt
) {}