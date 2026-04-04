package com.olivertech.orderservice.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload para criação de pedido")
public record OrderRequest(

        @Schema(description = "ID do cliente. Aceita letras, números, hífen e underscore.",
                example = "cust-abc-123",
                maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100, message = "customerId deve ter no máximo 100 caracteres")
        // Bloqueia XSS: rejeita < > " ' & e qualquer tag HTML/script
        @Pattern(
                regexp = "^[\\w\\-. @]+$",
                message = "customerId contém caracteres inválidos. Use apenas letras, números, hífen, ponto, espaço ou @"
        )
        String customerId,

        @Schema(description = "Valor em BRL", example = "299.90",
                minimum = "0.01", maximum = "9999999999999999.99",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive(message = "Valor deve ser positivo")
        @Digits(integer = 17, fraction = 2,
                message = "Valor deve ter no máximo 17 dígitos inteiros e 2 decimais")
        BigDecimal amount
) {}
