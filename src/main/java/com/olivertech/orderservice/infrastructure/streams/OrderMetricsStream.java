package com.olivertech.orderservice.infrastructure.streams;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.infrastructure.config.KafkaProperties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.time.Instant;

@Configuration
public class OrderMetricsStream {

    private static final Logger log =
            LoggerFactory.getLogger(OrderMetricsStream.class);

    private final KafkaProperties kafkaProperties;

    public OrderMetricsStream(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public KStream<String, OrderEvent> ordersStream(StreamsBuilder builder) {
        KStream<String, OrderEvent> ordersStream =
                builder.stream(kafkaProperties.topics().orders(),
                        Consumed.with(Serdes.String(), new JacksonJsonSerde<>(OrderEvent.class)));

        ordersStream
                .filter((key, event) -> event != null)
                .groupBy((key, event) -> event.status().name(),
                        Grouped.with(Serdes.String(), new JacksonJsonSerde<>(OrderEvent.class)))
                .count(Materialized.as("orders-by-status-store"))
                .toStream()
                .mapValues((status, count) -> new OrderMetric(status, count, Instant.now()))
                .to(kafkaProperties.topics().orderMetrics(),
                        Produced.with(Serdes.String(), new JacksonJsonSerde<>(OrderMetric.class)));

        log.info("Kafka Streams pipeline iniciado");
        return ordersStream;
    }
}
