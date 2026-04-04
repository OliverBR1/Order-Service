package com.olivertech.orderservice.application;

import com.olivertech.orderservice.application.usecase.ListOrdersUseCaseImpl;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListOrdersUseCaseImplTest {

    @Mock OrderReadRepositoryPort readRepo;
    @InjectMocks ListOrdersUseCaseImpl useCase;

    @Test
    void shouldReturnAllOrders() {
        Order o1 = Order.create("cust-1", new BigDecimal("10.00"));
        Order o2 = Order.create("cust-2", new BigDecimal("20.00"));
        when(readRepo.findAll()).thenReturn(List.of(o1, o2));

        List<Order> result = useCase.execute();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Order::getCustomerId)
                .containsExactly("cust-1", "cust-2");
    }

    @Test
    void shouldReturnEmptyListWhenNoOrders() {
        when(readRepo.findAll()).thenReturn(List.of());

        List<Order> result = useCase.execute();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPagedOrders() {
        Order o1 = Order.create("cust-1", new BigDecimal("10.00"));
        Order o2 = Order.create("cust-2", new BigDecimal("20.00"));
        when(readRepo.findAll(0, 10)).thenReturn(List.of(o1, o2));

        List<Order> result = useCase.execute(0, 10);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Order::getCustomerId)
                .containsExactly("cust-1", "cust-2");
    }

    @Test
    void shouldReturnEmptyPagedListWhenNoOrders() {
        when(readRepo.findAll(1, 5)).thenReturn(List.of());

        assertThat(useCase.execute(1, 5)).isEmpty();
    }
}

