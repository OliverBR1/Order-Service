package com.olivertech.orderservice.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.streams.application-id}") String appId,
            @Value("${spring.kafka.streams.state-dir}") String stateDir) {

        Map<String, Object> config = new HashMap<>();
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        config.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        config.put("default.deserialization.exception.handler", LogAndContinueExceptionHandler.class);

        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "1000");

        config.put(StreamsConfig.consumerPrefix(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG), "45000");
        config.put(StreamsConfig.consumerPrefix(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG), "3000");
        config.put(StreamsConfig.consumerPrefix(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG), "300000");

        config.put("reconnect.backoff.ms",     "1000");
        config.put("reconnect.backoff.max.ms", "10000");
        config.put("connections.max.idle.ms",  "540000");
        config.put("retry.backoff.ms",         "1000");
        config.put("retry.backoff.max.ms",     "10000");
        config.put(StreamsConfig.consumerPrefix(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG), "1000");
        config.put(StreamsConfig.consumerPrefix(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG), "10000");

        return new KafkaStreamsConfiguration(config);
    }
}
