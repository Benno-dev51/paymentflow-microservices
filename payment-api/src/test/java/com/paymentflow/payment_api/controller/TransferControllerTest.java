package com.paymentflow.payment_api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentflow.payment_api.dto.TransferRequest;
import com.paymentflow.payment_api.model.Transfer;
import com.paymentflow.payment_api.service.TransferProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferProducer transferProducer;

    @Test
    void serializesEventWithFieldsExpectedByExistingConsumers() throws Exception {
        UUID transferId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.parse("2026-01-15T10:30:00");
        Transfer transfer = Transfer.builder()
                .id(transferId)
                .idempotencyKey("request-123")
                .createdAt(createdAt)
                .build();

        JsonNode event = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .valueToTree(transfer);

        assertEquals(transferId.toString(), event.get("transferId").asText());
        assertTrue(event.hasNonNull("timestamp"));
        assertEquals("request-123", event.get("idempotencyKey").asText());
        assertFalse(event.has("id"));
        assertFalse(event.has("createdAt"));
    }

    @Test
    void acceptsValidTransferAndReturnsPendingResponse() throws Exception {
        UUID transferId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.parse("2026-01-15T10:30:00");
        Transfer transfer = Transfer.builder()
                .id(transferId)
                .idempotencyKey("request-123")
                .sourceAccount("1234567890")
                .destinationAccount("0987654321")
                .amount(new BigDecimal("25.50"))
                .status("PENDING")
                .createdAt(createdAt)
                .build();
        when(transferProducer.sendTransferEvent(any(TransferRequest.class), eq("request-123")))
                .thenReturn(transfer);

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", " request-123 ")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "0987654321",
                                  "amount": 25.50,
                                  "concept": "Test transfer"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transactionId").value(transferId.toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("request-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(transferProducer).sendTransferEvent(any(TransferRequest.class), eq("request-123"));
    }

    @Test
    void rejectsInvalidTransferWithoutPublishing() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "request-123")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "123",
                                  "destinationAccount": "0987654321",
                                  "amount": 500000.01,
                                  "concept": "Invalid transfer"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.sourceAccount").exists())
                .andExpect(jsonPath("$.details.amount").exists());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsTransferBetweenSameAccounts() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "request-123")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "1234567890",
                                  "amount": 25.50
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.differentAccounts").exists());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsAmountWithMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "request-123")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "0987654321",
                                  "amount": 25.501
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "request-123")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "destinationAccount": "",
                                  "concept": "Missing required values"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.sourceAccount").exists())
                .andExpect(jsonPath("$.details.destinationAccount").exists())
                .andExpect(jsonPath("$.details.amount").exists());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsAmountBelowMinimum() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "request-123")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "0987654321",
                                  "amount": 0.99
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsConceptLongerThan100Characters() throws Exception {
        String concept = "a".repeat(101);
        String requestBody = """
                {
                  "sourceAccount": "1234567890",
                  "destinationAccount": "0987654321",
                  "amount": 25.50,
                  "concept": "%s"
                }
                """.formatted(concept);

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "request-123")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.concept").exists());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsRequestWithoutIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "0987654321",
                                  "amount": 25.50,
                                  "concept": "Test transfer"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsBlankIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "   ")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "0987654321",
                                  "amount": 25.50
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El header 'Idempotency-Key' es obligatorio"));

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }

    @Test
    void rejectsIdempotencyKeyLongerThan128Characters() throws Exception {
        String idempotencyKey = "a".repeat(129);

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccount": "1234567890",
                                  "destinationAccount": "0987654321",
                                  "amount": 25.50
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El header 'Idempotency-Key' no puede exceder 128 caracteres"));

        verify(transferProducer, never()).sendTransferEvent(any(TransferRequest.class), any());
    }
}
