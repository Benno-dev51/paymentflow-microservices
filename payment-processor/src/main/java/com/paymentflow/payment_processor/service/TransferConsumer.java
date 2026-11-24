package com.paymentflow.payment_processor.service;


import com.paymentflow.payment_processor.model.Transfer;
import com.paymentflow.payment_processor.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferConsumer {
    private final TransferRepository transferRepository;
    private final KafkaTemplate<String, Transfer> kafkaTemplate; // <-- Agrega esto

    @KafkaListener(topics = "payment-events", groupId = "payment-validator-group", concurrency = "3")
    public void consumeTransferEvent(
            Transfer transfer,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("📥 [Partición {}] Evento consumido. ID: {}", partition, transfer.getTransferId());

        if (transfer.getAmount().doubleValue() > 50000) {
            transfer.setStatus("REJECTED");
        } else {
            transfer.setStatus("APPROVED");
        }

        Transfer savedTransfer = transferRepository.save(transfer);

        // Publica el resultado final para que otros microservicios reaccionen
        kafkaTemplate.send("payment-results", savedTransfer.getTransferId(), savedTransfer);
        log.info("📤 Resultado publicado en payment-results: {} ({})", savedTransfer.getTransferId(), savedTransfer.getStatus());
    }
}