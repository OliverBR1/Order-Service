package com.olivertech.orderservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private final KafkaProperties props;

    public KafkaTopicConfig(KafkaProperties props) {
        this.props = props;
    }

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(props.topics().orders())
                .partitions(props.partitions())
                .replicas(props.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .build();
    }

    @Bean
    public NewTopic dltTopic() {
        return TopicBuilder.name(props.topics().ordersDlt())
                .partitions(1)
                .replicas(props.replicas())
                .build();
    }

    @Bean
    public NewTopic retry0Topic() {
        return TopicBuilder.name(props.topics().ordersRetry0())
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }

    @Bean
    public NewTopic retry1Topic() {
        return TopicBuilder.name(props.topics().ordersRetry1())
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }

    @Bean
    public NewTopic retry2Topic() {
        return TopicBuilder.name(props.topics().ordersRetry2())
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }

    @Bean
    public NewTopic metricsTopic() {
        return TopicBuilder.name(props.topics().orderMetrics())
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }
}
