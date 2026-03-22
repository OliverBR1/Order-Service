package com.olivertech.orderservice.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(
        Topics topics,
        int    partitions,
        short  replicas
) {
    public record Topics(
            String orders,
            String ordersDlt,
            String ordersRetry0,
            String ordersRetry1,
            String ordersRetry2,
            String orderMetrics
    ) {}
}
