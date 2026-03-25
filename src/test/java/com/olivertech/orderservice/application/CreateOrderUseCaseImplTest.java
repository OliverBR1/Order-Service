package com.olivertech.orderservice.application;

import com.olivertech.orderservice.application.usecase.CreateOrderUseCaseImpl;
import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderEventPublisherPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCaseImpl")
class CreateOrderUseCaseImplTest {

    @Mock OrderWriteRepositoryPort writeRepo;
    @Mock OrderEventPublisherPort  publisher;
    @InjectMocks CreateOrderUseCaseImpl useCase;

    @Captor ArgumentCaptor<Order> orderCaptor;

    @BeforeEach
    void setup() {
        lenient().when(publisher.publish(any(Order.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldSaveBeforePublishing() {
        InOrder inOrder = inOrder(writeRepo, publisher);
        useCase.execute("c", BigDecimal.TEN);
        inOrder.verify(writeRepo).save(any(Order.class));
        inOrder.verify(publisher).publish(any(Order.class));
    }

    @Test
    void shouldReturnPendingStatus() {
        Order order = useCase.execute("c", BigDecimal.TEN);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldPublishEventWithSameIdAsSavedOrder() {
        useCase.execute("c", BigDecimal.TEN);
        verify(writeRepo).save(orderCaptor.capture());
        verify(publisher).publish(orderCaptor.capture());
        // ambos os captures devem ter o mesmo id
        assertThat(orderCaptor.getAllValues().get(0).getId())
                .isEqualTo(orderCaptor.getAllValues().get(1).getId());
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThatNullPointerException()
                .isThrownBy(() -> useCase.execute(null, BigDecimal.TEN));
        verifyNoInteractions(writeRepo, publisher);
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> useCase.execute("c", new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowEventPublishingExceptionOnKafkaFailure() {
        when(publisher.publish(any(Order.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker down")));
        assertThatThrownBy(() -> useCase.execute("c", BigDecimal.TEN))
                .isInstanceOf(EventPublishingException.class);
    }

    @Test
    void shouldNotPersistWhenDomainValidationFails() {
        assertThatThrownBy(() -> useCase.execute("c", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(writeRepo, publisher);
    }
}
