package com.paymentflow.payment_api.controller;



import com.paymentflow.payment_api.dto.TransferRequest;
import com.paymentflow.payment_api.dto.TransferResponse;
import com.paymentflow.payment_api.model.Transfer;
import com.paymentflow.payment_api.service.TransferProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferProducer transferProducer;

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("El header 'Idempotency-Key' es obligatorio");
        }

        String normalizedIdempotencyKey = idempotencyKey.trim();
        if (normalizedIdempotencyKey.length() > 128) {
            throw new IllegalArgumentException("El header 'Idempotency-Key' no puede exceder 128 caracteres");
        }

        Transfer event = transferProducer.sendTransferEvent(request, normalizedIdempotencyKey);

        TransferResponse response = new TransferResponse(
                event.getId(),
                event.getIdempotencyKey(),
                "PENDING",
                event.getSourceAccount(),
                event.getDestinationAccount(),
                event.getAmount(),
                event.getCreatedAt()
        );

        return ResponseEntity.accepted().body(response);
    }
}