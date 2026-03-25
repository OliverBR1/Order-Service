package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.domain.exception.EventPublishingException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturn400Message() {
        var result = handler.handleConstraintViolation(
                new ConstraintViolationException("ID inválido", Set.of()));
        assertThat(result).containsKey("error");
    }

    @Test
    void shouldReturn503WithoutKafkaDetails() {
        var result = handler.handlePublishingFailure(
                new EventPublishingException("Kafka offline", null));
        assertThat(result.get("error"))
                .doesNotContain("Kafka")
                .contains("indisponível");
    }

    @Test
    void shouldReturn405WithAllowedMethodsAndDocsLink() {
        var result = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("GET", List.of("POST")));
        assertThat(result)
                .containsKey("error")
                .containsEntry("allowed", "POST")
                .containsEntry("docs", "/swagger-ui.html");
        assertThat(result.get("error")).contains("GET");
    }

    @Test
    void shouldReturn500WithGenericMessage() {
        var result = handler.handleUnexpected(new RuntimeException("boom"));
        assertThat(result.get("error")).contains("Erro interno");
    }
}
