package com.olivertech.orderservice.application;

import com.olivertech.orderservice.application.usecase.FindOrderUseCaseImpl;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindOrderUseCaseImplTest {

    @Mock OrderReadRepositoryPort readRepo;
    @InjectMocks FindOrderUseCaseImpl useCase;

    @Test
    void shouldReturnOrderWhenFound() {
        Order order = Order.create("cust-1", new BigDecimal("75.00"));
        when(readRepo.findById(order.getId())).thenReturn(Optional.of(order));

        Optional<Order> result = useCase.execute(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(order.getId());
        assertThat(result.get().getCustomerId()).isEqualTo("cust-1");
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        when(readRepo.findById("ghost-id")).thenReturn(Optional.empty());

        assertThat(useCase.execute("ghost-id")).isEmpty();
    }
}

