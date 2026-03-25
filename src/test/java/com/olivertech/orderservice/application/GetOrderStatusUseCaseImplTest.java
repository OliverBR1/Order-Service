package com.olivertech.orderservice.application;

import com.olivertech.orderservice.application.usecase.GetOrderStatusUseCaseImpl;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
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
class GetOrderStatusUseCaseImplTest {

    @Mock OrderReadRepositoryPort readRepo;
    @InjectMocks GetOrderStatusUseCaseImpl useCase;

    @Test
    void shouldReturnPendingStatusWhenOrderFound() {
        Order order = Order.create("cust-1", BigDecimal.TEN);
        when(readRepo.findById(order.getId())).thenReturn(Optional.of(order));

        Optional<OrderStatus> result = useCase.execute(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldReturnProcessedStatusAfterProcessing() {
        Order order = Order.create("cust-1", BigDecimal.TEN);
        order.markAsProcessed();
        when(readRepo.findById(order.getId())).thenReturn(Optional.of(order));

        Optional<OrderStatus> result = useCase.execute(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(OrderStatus.PROCESSED);
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        when(readRepo.findById("unknown-id")).thenReturn(Optional.empty());

        assertThat(useCase.execute("unknown-id")).isEmpty();
    }
}

