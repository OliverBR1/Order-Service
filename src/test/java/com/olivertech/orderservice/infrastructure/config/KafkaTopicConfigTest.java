package com.olivertech.orderservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicConfigTest {

    KafkaTopicConfig config;

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
        KafkaProperties props = new KafkaProperties(topics, 3, (short) 1);
        config = new KafkaTopicConfig(props);
    }

    @Test
    void shouldCreateOrdersTopicWithCorrectName() {
        NewTopic topic = config.ordersTopic();
        assertThat(topic.name()).isEqualTo("orders-topic");
    }

    @Test
    void shouldCreateDltTopicWithCorrectName() {
        NewTopic topic = config.dltTopic();
        assertThat(topic.name()).isEqualTo("orders-topic.DLT");
    }

    @Test
    void shouldCreateRetry0TopicWithCorrectName() {
        NewTopic topic = config.retry0Topic();
        assertThat(topic.name()).isEqualTo("orders-topic-0");
    }

    @Test
    void shouldCreateRetry1TopicWithCorrectName() {
        NewTopic topic = config.retry1Topic();
        assertThat(topic.name()).isEqualTo("orders-topic-1");
    }

    @Test
    void shouldCreateRetry2TopicWithCorrectName() {
        NewTopic topic = config.retry2Topic();
        assertThat(topic.name()).isEqualTo("orders-topic-2");
    }

    @Test
    void shouldCreateMetricsTopicWithCorrectName() {
        NewTopic topic = config.metricsTopic();
        assertThat(topic.name()).isEqualTo("order-metrics-topic");
    }
}

