package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderWriteRepositoryAdapterTest {

    @Mock JpaOrderRepository jpaRepo;

    OrderWriteRepositoryAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new OrderWriteRepositoryAdapter(jpaRepo);
    }

    @Test
    void save_shouldDelegateToJpaRepository() {
        Order order = Order.create("cust-1", new BigDecimal("99.00"));

        adapter.save(order);

        verify(jpaRepo).save(any(OrderEntity.class));
    }

    @Test
    void updateStatus_shouldPassStatusNameToJpaRepository() {
        adapter.updateStatus("order-id-1", OrderStatus.PROCESSED);

        verify(jpaRepo).updateStatus(eq("order-id-1"), eq("PROCESSED"));
    }

    @Test
    void updateStatus_shouldWorkForFailedStatus() {
        adapter.updateStatus("order-id-2", OrderStatus.FAILED);

        verify(jpaRepo).updateStatus(eq("order-id-2"), eq("FAILED"));
    }
}

