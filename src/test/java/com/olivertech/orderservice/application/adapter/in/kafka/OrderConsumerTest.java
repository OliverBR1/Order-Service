package com.olivertech.orderservice.application.adapter.in.kafka;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.ProcessOrderUseCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock ProcessOrderUseCase processUC;

    SimpleMeterRegistry registry;
    OrderConsumer consumer;
    OrderEvent event;

    @BeforeEach
    void setup() {
        registry = new SimpleMeterRegistry();
        consumer = new OrderConsumer(processUC, registry, "orders");
        event = new OrderEvent("o1", "c1", BigDecimal.TEN, OrderStatus.PENDING, Instant.now());
    }

    @Test
    void shouldProcessAndIncrementSuccess() {
        consumer.consume(event, 0, 1L);

        verify(processUC).execute(event.orderId());
        assertThat(registry.get("kafka.consumer.success").tag("topic", "orders").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void shouldIncrementErrorAndRethrow() {
        doThrow(new RuntimeException("erro")).when(processUC).execute(anyString());

        assertThatThrownBy(() -> consumer.consume(event, 0, 1L))
                .isInstanceOf(RuntimeException.class);

        assertThat(registry.get("kafka.consumer.error").tag("topic", "orders").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void shouldNeverIncrementSuccessOnFailure() {
        doThrow(new RuntimeException()).when(processUC).execute(anyString());

        assertThatThrownBy(() -> consumer.consume(event, 0, 0L));

        assertThat(registry.find("kafka.consumer.success").counter()).isNull();
    }

    @Test
    void shouldIncrementDltCounterOnDeadLetter() {
        consumer.handleDlt(event, "orders.DLT");

        assertThat(registry.get("kafka.consumer.dlt").tag("topic", "orders.DLT").counter().count())
                .isEqualTo(1.0);
    }
}