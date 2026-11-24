package com.paymentflow.payment_processor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // Envia el mensaje fallido al mismo nombre del tópico con el sufijo .DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // Configuramos 2 reintentos, esperando 1 segundo (1000ms) entre cada uno.
        // Si después de los 2 reintentos sigue fallando, se ejecuta el recoverer (DLQ).
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}