package com.olivertech.orderservice.application.adapter.out.kafka;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.out.OrderEventPublisherPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Component
public class KafkaOrderEventPublisher implements OrderEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${kafka.topics.orders}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public CompletableFuture<Void> publish(Order order) {
        OrderEvent event = OrderEvent.from(order);

        ProducerRecord<String, OrderEvent> record = new ProducerRecord<>(topic, order.getId(), event);

        record.headers()
                .add("source", "order-service".getBytes(StandardCharsets.UTF_8))
                .add("eventType", "ORDER_CREATED".getBytes(StandardCharsets.UTF_8));

        return kafkaTemplate.send(record)
                .thenApply(result -> {
                    log.info("Publicado: orderId={} partition={} offset={}",
                            order.getId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return (Void) null;
                });
    }
}
