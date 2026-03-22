package com.olivertech.orderservice.infrastructure.streams;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.model.OrderStatus;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Configuration
public class OrderMetricsStream {

    private static final Logger log =
            LoggerFactory.getLogger(OrderMetricsStream.class);

    // @Bean com parâmetro injetado — substitui @Component + @Autowired (sem Lombok)
    @Bean
    public KStream<String, OrderEvent> ordersStream(StreamsBuilder builder) {
        KStream<String, OrderEvent> ordersStream =
                builder.stream("orders-topic",
                        Consumed.with(Serdes.String(), new JsonSerde<>(OrderEvent.class)));

        // Conta pedidos por status (KTable stateful)
        KTable<String, Long> ordersByStatus = ordersStream
                .filter((key, event) -> event != null)
                .groupBy((key, event) -> event.status().name(),
                        Grouped.with(Serdes.String(), new JsonSerde<>(OrderEvent.class)))
                .count(Materialized.as("orders-by-status-store"));

        // Soma de valor por cliente (janela de 1 hora)
        ordersStream
                .filter((key, event) -> event.status() == OrderStatus.PENDING)
                .groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(OrderEvent.class)))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
                .aggregate(
                        () -> BigDecimal.ZERO,
                        (key, event, acc) -> acc.add(event.amount()),
                        Materialized.<String, BigDecimal, WindowStore<Bytes, byte[]>>as(
                                        "revenue-per-customer-store")
                                .withValueSerde(new JsonSerde<>(BigDecimal.class))
                );

        // Publica métricas no topic de saída
        ordersByStatus.toStream()
                .mapValues((status, count) -> new OrderMetric(status, count, Instant.now()))
                .to("order-metrics-topic",
                        Produced.with(Serdes.String(), new JsonSerde<>(OrderMetric.class)));

        log.info("Kafka Streams pipeline iniciado");
        return ordersStream;
    }
}
