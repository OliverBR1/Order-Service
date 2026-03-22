package com.olivertech.orderservice.application.dto;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de criação de pedido")
public record OrderResponse(

        @Schema(description = "UUID do pedido gerado")
        String orderId,

        @Schema(description = "Status inicial", example = "PENDING")
        OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus());
    }
}

