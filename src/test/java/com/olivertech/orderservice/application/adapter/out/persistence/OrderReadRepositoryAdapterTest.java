package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReadRepositoryAdapterTest {

    @Mock JpaOrderRepository jpaRepo;

    OrderReadRepositoryAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new OrderReadRepositoryAdapter(jpaRepo);
    }

    @Test
    void findById_shouldReturnMappedDomainOrder() {
        Order order  = Order.create("cust-1", new BigDecimal("50.00"));
        OrderEntity entity = OrderEntity.from(order);
        when(jpaRepo.findById(order.getId())).thenReturn(Optional.of(entity));

        Optional<Order> result = adapter.findById(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(order.getId());
        assertThat(result.get().getCustomerId()).isEqualTo("cust-1");
        assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(jpaRepo.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void existsByIdAndStatus_shouldReturnTrueWhenExists() {
        when(jpaRepo.existsByIdAndStatus("id-1", "PENDING")).thenReturn(true);

        assertThat(adapter.existsByIdAndStatus("id-1", OrderStatus.PENDING)).isTrue();
    }

    @Test
    void existsByIdAndStatus_shouldReturnFalseWhenNotExists() {
        when(jpaRepo.existsByIdAndStatus("id-1", "PROCESSED")).thenReturn(false);

        assertThat(adapter.existsByIdAndStatus("id-1", OrderStatus.PROCESSED)).isFalse();
    }

    @Test
    void findAll_shouldReturnMappedDomainOrders() {
        Order o1 = Order.create("cust-1", new BigDecimal("10.00"));
        Order o2 = Order.create("cust-2", new BigDecimal("20.00"));
        when(jpaRepo.findAll()).thenReturn(List.of(OrderEntity.from(o1), OrderEntity.from(o2)));

        List<Order> result = adapter.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Order::getCustomerId)
                .containsExactlyInAnyOrder("cust-1", "cust-2");
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoOrders() {
        when(jpaRepo.findAll()).thenReturn(List.of());

        assertThat(adapter.findAll()).isEmpty();
    }
}

