package com.olivertech.orderservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final String id;
    private final String customerId;
    private final BigDecimal amount;
    private OrderStatus status;
    private final Instant createdAt;

    private Order(String id, String customerId, BigDecimal amount, Instant createdAt) {
        this.id = id; this.customerId = customerId;
        this.amount = amount; this.status = OrderStatus.PENDING;
        this.createdAt = createdAt;
    }

    // Factory method para criação de novo pedido
    public static Order create(String customerId, BigDecimal amount) {
        Objects.requireNonNull(customerId, "customerId obrigatório");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Valor deve ser positivo");
        return new Order(UUID.randomUUID().toString(), customerId, amount, Instant.now());
    }

    // Factory method para reconstituição a partir de persistência (sem regras de criação)
    public static Order reconstitute(String id, String customerId,
                                     BigDecimal amount, OrderStatus status,
                                     Instant createdAt) {
        Order order = new Order(id, customerId, amount, createdAt);
        order.status = status;
        return order;
    }

    public void markAsProcessed() {
        if (this.status != OrderStatus.PENDING)
            throw new IllegalStateException("Apenas pedidos PENDING podem ser processados");
        this.status = OrderStatus.PROCESSED;
    }

    public String getId()          { return id; }
    public String getCustomerId()  { return customerId; }
    public BigDecimal getAmount()  { return amount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt()  { return createdAt; }
}
