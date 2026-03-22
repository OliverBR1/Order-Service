package com.olivertech.orderservice.application.adapter.in.kafka;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.ProcessOrderUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.internal.util.Timer;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.*;
import static scala.Option.when;


@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock
    ProcessOrderUseCase processUC;
    @Mock
    MeterRegistry registry;
    @Mock Counter             successCounter;
    @Mock
    Counter errorCounter;
    @InjectMocks
    OrderConsumer consumer;

    OrderEvent event;

    @BeforeEach
    void setup() {
        event = new OrderEvent("o1","c1", BigDecimal.TEN, OrderStatus.PENDING, Instant.now());
        when(registry.counter(eq("kafka.consumer.success"), any(String[].class)))
                .thenReturn(successCounter);
        when(registry.counter(eq("kafka.consumer.error"), any(String[].class)))
                .thenReturn(errorCounter);
    }

    @Test
    void shouldProcessAndIncrementSuccess() {
        try (MockedStatic<Timer> mt = mockStatic(Timer.class)) {
            Timer.Sample sample = mock(Timer.Sample.class);
            mt.when(() -> Timer.start(registry)).thenReturn(sample);

            consumer.consume(event, 0, 1L);

            verify(processUC).execute(event);
            verify(successCounter).increment();
            verifyNoInteractions(errorCounter);
        }
    }
    @Test void shouldIncrementErrorAndRethrow() {
        try (MockedStatic<Timer> mt = mockStatic(Timer.class)) {
            mt.when(() -> Timer.start(registry)).thenReturn(mock(Timer.Sample.class));
            doThrow(new RuntimeException("erro")).when(processUC).execute(any());

            assertThatThrownBy(() -> consumer.consume(event, 0, 1L))
                    .isInstanceOf(RuntimeException.class);
            verify(errorCounter).increment();
        }
    }
    @Test void shouldNeverIncrementSuccessOnFailure() {
        try (MockedStatic<Timer> mt = mockStatic(Timer.class)) {
            mt.when(() -> Timer.start(registry)).thenReturn(mock(Timer.Sample.class));
            doThrow(new RuntimeException()).when(processUC).execute(any());

            assertThatThrownBy(() -> consumer.consume(event, 0, 0L));
            verify(successCounter, never()).increment();
        }
    }
}
