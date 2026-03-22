package com.olivertech.orderservice.application.adapter.out;

import com.olivertech.orderservice.application.adapter.out.kafka.KafkaOrderEventPublisher;
import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.model.OrderStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static scala.Option.when;

@ExtendWith(MockitoExtension.class)
class KafkaOrderEventPublisherTest {

    @Mock
    KafkaTemplate<String, OrderEvent> kafkaTemplate;

    KafkaOrderEventPublisher publisher;

    OrderEvent event;

    @BeforeEach
    void setup() {
        publisher = new KafkaOrderEventPublisher(kafkaTemplate, "orders-topic");
        event = new OrderEvent("ord-1","cust-1", BigDecimal.TEN,
                OrderStatus.PENDING, Instant.now());
    }

    @Test void shouldSendWithOrderIdAsKey() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));
        publisher.publish(event);
        verify(kafkaTemplate).send(argThat((ProducerRecord<String, OrderEvent> r) ->
                "ord-1".equals(r.key()) && event.equals(r.value())));
    }

    @Test
    void shouldAddHeadersWithExplicitUtf8() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));
        publisher.publish(event);
        verify(kafkaTemplate).send(argThat((ProducerRecord<String,OrderEvent> r) -> {
            Header src = r.headers().lastHeader("source");
            Header evt = r.headers().lastHeader("eventType");
            return src != null
                    && "order-service".equals(new String(src.value(), UTF_8))
                    && "ORDER_CREATED".equals(new String(evt.value(), UTF_8));
        }));
    }

    @Test void shouldReturnFailedFutureOnBrokerError() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));
        assertThat(publisher.publish(event)).isCompletedExceptionally();
    }

    SendResult<String,OrderEvent> mockSendResult() {
        return new SendResult<>(null,
                new RecordMetadata(new TopicPartition("orders-topic",0),0,0,0,0,0));
    }
}
