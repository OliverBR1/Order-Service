package com.olivertech.orderservice.infrastructure.config;

import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaStreamsConfigTest {

    KafkaStreamsConfig config = new KafkaStreamsConfig();

    @Test
    void shouldSetBootstrapServers() {
        KafkaStreamsConfiguration result =
                config.kafkaStreamsConfig("localhost:9092", "test-app", "/tmp/state");

        Properties props = result.asProperties();
        assertThat(props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo("localhost:9092");
    }

    @Test
    void shouldSetApplicationId() {
        KafkaStreamsConfiguration result =
                config.kafkaStreamsConfig("localhost:9092", "my-app-id", "/tmp/state");

        assertThat(result.asProperties().getProperty(StreamsConfig.APPLICATION_ID_CONFIG))
                .isEqualTo("my-app-id");
    }

    @Test
    void shouldSetStateDir() {
        KafkaStreamsConfiguration result =
                config.kafkaStreamsConfig("localhost:9092", "test-app", "/custom/state/dir");

        assertThat(result.asProperties().getProperty(StreamsConfig.STATE_DIR_CONFIG))
                .isEqualTo("/custom/state/dir");
    }

    @Test
    void shouldSetExactlyOnceV2ProcessingGuarantee() {
        KafkaStreamsConfiguration result =
                config.kafkaStreamsConfig("localhost:9092", "test-app", "/tmp/state");

        assertThat(result.asProperties().getProperty(StreamsConfig.PROCESSING_GUARANTEE_CONFIG))
                .isEqualTo(StreamsConfig.EXACTLY_ONCE_V2);
    }

    @Test
    void shouldSetCommitInterval() {
        KafkaStreamsConfiguration result =
                config.kafkaStreamsConfig("localhost:9092", "test-app", "/tmp/state");

        assertThat(result.asProperties().getProperty(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG))
                .isEqualTo("1000");
    }

    @Test
    void shouldReturnNonNullConfiguration() {
        KafkaStreamsConfiguration result =
                config.kafkaStreamsConfig("broker:9092", "app", "/state");

        assertThat(result).isNotNull();
        assertThat(result.asProperties()).isNotEmpty();
    }
}

