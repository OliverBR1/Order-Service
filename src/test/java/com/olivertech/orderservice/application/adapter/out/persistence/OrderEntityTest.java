package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OrderEntityTest {

    @Test
    void from_shouldMapAllFieldsFromDomainOrder() {
        Order order = Order.create("cust-99", new BigDecimal("149.99"));

        OrderEntity entity = OrderEntity.from(order);
        Order restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo(order.getId());
        assertThat(restored.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(restored.getAmount()).isEqualByComparingTo(order.getAmount());
        assertThat(restored.getStatus()).isEqualTo(order.getStatus());
        assertThat(restored.getCreatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void from_shouldPreserveProcessedStatus() {
        Order order = Order.create("cust-1", BigDecimal.TEN);
        order.markAsProcessed();

        Order restored = OrderEntity.from(order).toDomain();

        assertThat(restored.getStatus()).isEqualTo(OrderStatus.PROCESSED);
    }

    @Test
    void toDomain_shouldReconstitutePendingOrder() {
        Order original = Order.create("cust-2", new BigDecimal("0.01"));

        Order reconstituted = OrderEntity.from(original).toDomain();

        assertThat(reconstituted.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(reconstituted.getCustomerId()).isEqualTo("cust-2");
    }
}

