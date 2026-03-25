package com.olivertech.orderservice.application;

import com.olivertech.orderservice.application.usecase.ProcessOrderUseCaseImpl;
import com.olivertech.orderservice.domain.exception.OrderNotFoundException;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOrderUseCaseImplTest {

    @Mock OrderReadRepositoryPort  readRepo;
    @Mock OrderWriteRepositoryPort writeRepo;
    @InjectMocks ProcessOrderUseCaseImpl useCase;

    Order  pendingOrder;
    String orderId;

    @BeforeEach
    void setup() {
        pendingOrder = Order.create("cust-1", BigDecimal.TEN);
        orderId      = pendingOrder.getId();
    }

    @Test
    void shouldProcessAndUpdateStatus() {
        when(readRepo.existsByIdAndStatus(orderId, OrderStatus.PROCESSED)).thenReturn(false);
        when(readRepo.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        useCase.execute(orderId);

        verify(writeRepo).updateStatus(orderId, OrderStatus.PROCESSED);
    }

    @Test
    void shouldIgnoreDuplicateEvent() {
        when(readRepo.existsByIdAndStatus(orderId, OrderStatus.PROCESSED)).thenReturn(true);

        useCase.execute(orderId);

        verifyNoInteractions(writeRepo);
        verify(readRepo, never()).findById(any());
    }

    @Test
    void shouldThrowForUnknownOrder() {
        when(readRepo.existsByIdAndStatus(any(), any())).thenReturn(false);
        when(readRepo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(orderId))
                .isInstanceOf(OrderNotFoundException.class);
        verify(writeRepo, never()).updateStatus(any(), any());
    }

    @Test
    void shouldCallMarkAsProcessedOnDomain() {
        when(readRepo.existsByIdAndStatus(any(), any())).thenReturn(false);
        when(readRepo.findById(any())).thenReturn(Optional.of(pendingOrder));

        useCase.execute(orderId);

        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.PROCESSED);
    }
}
