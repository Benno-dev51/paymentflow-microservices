package com.paymentflow.payment_api.controller;



import com.paymentflow.payment_api.model.Transfer;
import com.paymentflow.payment_api.service.TransferProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferProducer transferProducer;

    @PostMapping
    public ResponseEntity<String> initiateTransfer(@RequestBody Transfer transfer) {
        transfer.setTimestamp(LocalDateTime.now());

        // Delega la operación al productor de Kafka
        transferProducer.sendTransferEvent(transfer);

        return ResponseEntity.ok("Transferencia recibida y encolada para procesamiento");
    }
}