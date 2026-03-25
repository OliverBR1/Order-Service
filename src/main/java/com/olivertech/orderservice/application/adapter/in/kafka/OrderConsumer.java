package com.olivertech.orderservice.application.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonParseException;
import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.domain.port.in.ProcessOrderUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderConsumer.class);

    private final ProcessOrderUseCase processOrderUseCase;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topics.orders}")
    private String ordersTopic;

    public OrderConsumer(ProcessOrderUseCase processOrderUseCase,
                         MeterRegistry meterRegistry) {
        this.processOrderUseCase = processOrderUseCase;
        this.meterRegistry       = meterRegistry;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 30000),
            autoCreateTopics = "false",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            exclude = { DeserializationException.class, JsonParseException.class },
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "${kafka.topics.orders}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumindo orderId={} partition={} offset={}",
                event.orderId(), partition, offset);

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Extrai apenas o orderId — o domínio não precisa conhecer OrderEvent
            processOrderUseCase.execute(event.orderId());
            sample.stop(meterRegistry.timer("kafka.consumer.processing.time",
                    "topic", ordersTopic));
            meterRegistry.counter("kafka.consumer.success",
                    "topic", ordersTopic).increment();
        } catch (Exception ex) {
            meterRegistry.counter("kafka.consumer.error",
                    "topic", ordersTopic).increment();
            throw ex;
        }
    }

    @DltHandler
    public void handleDlt(OrderEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] orderId={} esgotou retries. Topic={}", event.orderId(), topic);
        meterRegistry.counter("kafka.consumer.dlt", "topic", topic).increment();
    }
}
