package com.olivertech.orderservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEvent(
        @NotBlank String      orderId,
        @NotBlank  String      customerId,
        @NotNull BigDecimal amount,
        @NotNull OrderStatus status,
        Instant     occurredAt
) {
    public static OrderEvent from(Order order) {
        return new OrderEvent(order.getId(), order.getCustomerId(),
                order.getAmount(), order.getStatus(), Instant.now());
    }
}
