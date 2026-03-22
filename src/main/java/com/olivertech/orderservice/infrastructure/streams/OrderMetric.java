package com.olivertech.orderservice.infrastructure.streams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderMetric(
        String  status,
        Long    count,
        Instant calculatedAt
) {}

