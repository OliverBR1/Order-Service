package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 100, nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 50, nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    protected OrderEntity() {}

    public static OrderEntity from(Order order) {
        OrderEntity e = new OrderEntity();
        e.id         = order.getId();
        e.customerId = order.getCustomerId();
        e.amount     = order.getAmount();
        e.status     = order.getStatus().name();
        e.createdAt  = order.getCreatedAt();
        return e;
    }

    public Order toDomain() {
        return Order.reconstitute(id, customerId, amount,
                OrderStatus.valueOf(status), createdAt);
    }
}

