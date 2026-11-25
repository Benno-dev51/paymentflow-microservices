package com.paymentflow.payment_api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "La cuenta origen es obligatoria")
        @Pattern(regexp = "^[0-9]{10,18}$", message = "La cuenta origen debe tener entre 10 y 18 dígitos")
        String sourceAccount,

        @NotBlank(message = "La cuenta destino es obligatoria")
        @Pattern(regexp = "^[0-9]{10,18}$", message = "La cuenta destino debe tener entre 10 y 18 dígitos")
        String destinationAccount,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "1.00", message = "El monto mínimo de transferencia es $1.00")
        @DecimalMax(value = "500000.00", message = "El monto máximo por operación es $500,000.00")
        @Digits(integer = 6, fraction = 2, message = "El monto debe tener como máximo dos decimales")
        BigDecimal amount,

        @Size(max = 100, message = "El concepto no puede exceder 100 caracteres")
        String concept
) {
    @AssertTrue(message = "La cuenta destino no puede ser igual a la cuenta origen")
    public boolean isDifferentAccounts() {
        if (sourceAccount == null || destinationAccount == null) {
            return true; // @NotBlank se encarga de reportar el nulo/vacío
        }
        return !sourceAccount.trim().equals(destinationAccount.trim());
    }
}