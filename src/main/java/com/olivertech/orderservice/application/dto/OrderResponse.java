package com.olivertech.orderservice.application.dto;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resposta de pedido")
public record OrderResponse(

        @Schema(description = "UUID do pedido gerado")
        String orderId,

        @Schema(description = "ID do cliente")
        String customerId,

        @Schema(description = "Valor em BRL", example = "299.90")
        BigDecimal amount,

        @Schema(description = "Status do pedido", example = "PENDING")
        OrderStatus status,

        @Schema(description = "Data/hora de criação (UTC)")
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt());
    }
}

