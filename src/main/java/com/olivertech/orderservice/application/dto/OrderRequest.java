package com.olivertech.orderservice.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Payload para criação de pedido")
public record OrderRequest(

        @Schema(description = "ID do cliente", example = "cust-abc-123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String customerId,

        @Schema(description = "Valor em BRL", example = "299.90",
                minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive BigDecimal amount
) {}
