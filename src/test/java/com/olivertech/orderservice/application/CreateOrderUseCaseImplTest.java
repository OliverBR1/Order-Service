package com.olivertech.orderservice.application;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.application.dto.OrderRequest;
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

    @Mock
    OrderWriteRepositoryPort writeRepo;
    @Mock
    OrderEventPublisherPort publisher;
    @InjectMocks
    CreateOrderUseCaseImpl useCase;
    @Captor ArgumentCaptor<Order>       orderCaptor;
    @Captor
    ArgumentCaptor<OrderEvent> eventCaptor;

    @BeforeEach
    void setup() {
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test void shouldSaveBeforePublishing() {
        InOrder order = inOrder(writeRepo, publisher);
        useCase.execute(new OrderRequest("c", BigDecimal.TEN));
        order.verify(writeRepo).save(any(Order.class));
        order.verify(publisher).publish(any(OrderEvent.class));
    }
    @Test
    void shouldReturnPendingStatus() {
        assertThat(useCase.execute(new OrderRequest("c", BigDecimal.TEN)).status())
                .isEqualTo(OrderStatus.PENDING);
    }
    @Test void shouldPublishEventWithSameIdAsSavedOrder() {
        useCase.execute(new OrderRequest("c", BigDecimal.TEN));
        verify(writeRepo).save(orderCaptor.capture());
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId())
                .isEqualTo(orderCaptor.getValue().getId());
    }
    @Test void shouldRejectNullCustomerId() {
        assertThatNullPointerException()
                .isThrownBy(() -> useCase.execute(new OrderRequest(null, BigDecimal.TEN)));
        verifyNoInteractions(writeRepo, publisher);
    }
    @Test void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> useCase.execute(new OrderRequest("c", new BigDecimal("-1"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void shouldThrowEventPublishingExceptionOnKafkaFailure() {
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker down")));
        assertThatThrownBy(() -> useCase.execute(new OrderRequest("c", BigDecimal.TEN)))
                .isInstanceOf(EventPublishingException.class);
    }
    @Test void shouldNotPersistWhenDomainValidationFails() {
        assertThatThrownBy(() -> useCase.execute(new OrderRequest(null, BigDecimal.TEN)));
        verifyNoInteractions(writeRepo);
    }
}
