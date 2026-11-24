package com.paymentflow.payment_api.service;
import com.paymentflow.payment_api.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferProducer {

    // KafkaTemplate ya sabe cómo serializar a JSON gracias a tu application.yml
    private final KafkaTemplate<String, Transfer> kafkaTemplate;
    private static final String TOPIC = "payment-events";

    public void sendTransferEvent(Transfer transfer) {
        // Simulamos la asignación de un ID de transacción si no viene en la petición
        if (transfer.getTransferId() == null) {
            transfer.setTransferId(UUID.randomUUID().toString());
        }

        transfer.setStatus("PENDING");

        log.info("Enviando evento de transferencia al tópico {}: {}", TOPIC, transfer.getTransferId());

        // Enviamos el mensaje a Kafka. La llave (Key) es el ID para asegurar el orden por transferencia.
        kafkaTemplate.send(TOPIC, transfer.getTransferId(), transfer);
    }
}