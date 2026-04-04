package com.olivertech.orderservice.infrastructure.streams;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.infrastructure.config.KafkaProperties;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.ValueMapperWithKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class OrderMetricsStreamTest {

    @Mock StreamsBuilder builder;

    OrderMetricsStream stream;

    @BeforeEach
    void setup() {
        KafkaProperties.Topics topics = new KafkaProperties.Topics(
                "orders-topic",
                "orders-topic.DLT",
                "orders-topic-0",
                "orders-topic-1",
                "orders-topic-2",
                "order-metrics-topic"
        );
        stream = new OrderMetricsStream(new KafkaProperties(topics, 3, (short) 1));
    }

    private void stubChain(KStream kStream, KStream filtered,
                           KGroupedStream grouped, KTable table,
                           KStream tableStream, KStream mapped) {
        when(builder.stream(any(String.class), any(Consumed.class))).thenReturn(kStream);
        when(kStream.filter(any())).thenReturn(filtered);
        when(filtered.groupBy(any(), any(Grouped.class))).thenReturn(grouped);
        when(grouped.count(any(Materialized.class))).thenReturn(table);
        when(table.toStream()).thenReturn(tableStream);
        // mapValues com lambda (k, v) -> ... é ValueMapperWithKey — cast para desambiguar
        doReturn(mapped).when(tableStream).mapValues((ValueMapperWithKey) any());
    }

    @Test
    void shouldReadFromOrdersTopic() {
        KStream kStream   = mock(KStream.class);
        KStream filtered  = mock(KStream.class);
        KGroupedStream grouped = mock(KGroupedStream.class);
        KTable  table     = mock(KTable.class);
        KStream tableStream = mock(KStream.class);
        KStream mapped    = mock(KStream.class);

        stubChain(kStream, filtered, grouped, table, tableStream, mapped);

        KStream<String, OrderEvent> result = stream.ordersStream(builder);

        assertThat(result).isSameAs(kStream);
        verify(builder).stream(eq("orders-topic"), any(Consumed.class));
    }

    @Test
    void shouldWriteToMetricsTopic() {
        KStream kStream   = mock(KStream.class);
        KStream filtered  = mock(KStream.class);
        KGroupedStream grouped = mock(KGroupedStream.class);
        KTable  table     = mock(KTable.class);
        KStream tableStream = mock(KStream.class);
        KStream mapped    = mock(KStream.class);

        stubChain(kStream, filtered, grouped, table, tableStream, mapped);

        stream.ordersStream(builder);

        verify(mapped).to(eq("order-metrics-topic"), any());
    }
}
