package com.paymentflow.payment_api.service;

import com.paymentflow.payment_api.dto.TransferRequest;
import com.paymentflow.payment_api.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferProducer {

    private final KafkaTemplate<String, Transfer> kafkaTemplate;
    private static final String TOPIC = "payment-events";

    public Transfer sendTransferEvent(TransferRequest request, String idempotencyKey) {
        Transfer transferEvent = Transfer.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .sourceAccount(request.sourceAccount())
                .destinationAccount(request.destinationAccount())
                .amount(request.amount())
                .concept(request.concept())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Publicando transferencia {} con Idempotency-Key {} al topic {}",
                transferEvent.getId(), idempotencyKey, TOPIC);

        kafkaTemplate.send(TOPIC, transferEvent.getId().toString(), transferEvent);
        return transferEvent;
    }
}