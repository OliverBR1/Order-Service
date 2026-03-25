package com.olivertech.orderservice.application.adapter.in.kafka;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.ProcessOrderUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock ProcessOrderUseCase processUC;
    @Mock MeterRegistry registry;
    @Mock Counter successCounter;
    @Mock Counter errorCounter;
    @Mock Timer processingTimer;           // <-- novo: para o sample.stop(...)
    @InjectMocks OrderConsumer consumer;

    OrderEvent event;

    @BeforeEach
    void setup() {
        // injeta o @Value manualmente
        ReflectionTestUtils.setField(consumer, "ordersTopic", "orders");

        event = new OrderEvent("o1", "c1", BigDecimal.TEN, OrderStatus.PENDING, Instant.now());

        // varargs correto: anyString(), anyString() em vez de any(String[].class)
        lenient().when(registry.counter(eq("kafka.consumer.success"), anyString(), anyString()))
                .thenReturn(successCounter);
        lenient().when(registry.counter(eq("kafka.consumer.error"), anyString(), anyString()))
                .thenReturn(errorCounter);

        // evita NPE no sample.stop(meterRegistry.timer(...))
        lenient().when(registry.timer(anyString(), anyString(), anyString()))
                .thenReturn(processingTimer);
    }

    @Test
    void shouldProcessAndIncrementSuccess() {
        try (MockedStatic<Timer> mt = mockStatic(Timer.class)) {
            Timer.Sample sample = mock(Timer.Sample.class);
            mt.when(() -> Timer.start(registry)).thenReturn(sample);

            consumer.consume(event, 0, 1L);

            verify(processUC).execute(event.orderId());
            verify(successCounter).increment();
            verifyNoInteractions(errorCounter);
        }
    }

    @Test
    void shouldIncrementErrorAndRethrow() {
        try (MockedStatic<Timer> mt = mockStatic(Timer.class)) {
            mt.when(() -> Timer.start(registry)).thenReturn(mock(Timer.Sample.class));
            doThrow(new RuntimeException("erro")).when(processUC).execute(anyString());

            assertThatThrownBy(() -> consumer.consume(event, 0, 1L))
                    .isInstanceOf(RuntimeException.class);
            verify(errorCounter).increment();
        }
    }

    @Test
    void shouldNeverIncrementSuccessOnFailure() {
        try (MockedStatic<Timer> mt = mockStatic(Timer.class)) {
            mt.when(() -> Timer.start(registry)).thenReturn(mock(Timer.Sample.class));
            doThrow(new RuntimeException()).when(processUC).execute(anyString());

            assertThatThrownBy(() -> consumer.consume(event, 0, 0L));
            verify(successCounter, never()).increment();
        }
    }

    @Test
    void shouldIncrementDltCounterOnDeadLetter() {
        Counter dltCounter = mock(Counter.class);
        when(registry.counter(eq("kafka.consumer.dlt"), anyString(), anyString()))
                .thenReturn(dltCounter);

        consumer.handleDlt(event, "orders.DLT");

        verify(dltCounter).increment();
    }
}