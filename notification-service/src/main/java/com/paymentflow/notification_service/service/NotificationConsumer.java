package com.paymentflow.notification_service.service;

import com.paymentflow.notification_service.model.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationConsumer {

    @KafkaListener(topics = "payment-results", groupId = "notification-group")
    public void sendNotification(Transfer transfer) {
        if ("APPROVED".equals(transfer.getStatus())) {
            log.info("📧 [EMAIL ENVIADO] Transferencia {} por ${} APROBADA hacia la cuenta {}.",
                    transfer.getTransferId(), transfer.getAmount(), transfer.getDestinationAccount());
        } else {
            log.warn("⚠️ [ALERTA ENVIADA] Transferencia {} por ${} RECHAZADA por superar el límite permitido.",
                    transfer.getTransferId(), transfer.getAmount());
        }
    }
}