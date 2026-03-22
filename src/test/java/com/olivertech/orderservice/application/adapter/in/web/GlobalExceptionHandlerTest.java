package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.domain.exception.EventPublishingException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturn400Message() {
        var result = handler.handleConstraintViolation(
                new ConstraintViolationException("ID inválido", Set.of()));
        assertThat(result).containsKey("error");
    }
    @Test void shouldReturn503WithoutKafkaDetails() {
        var result = handler.handlePublishingFailure(
                new EventPublishingException("Kafka offline", null));
        assertThat(result.get("error"))
                .doesNotContain("Kafka")
                .contains("indisponível");
    }
}
