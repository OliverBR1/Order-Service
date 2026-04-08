package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.exception.OrderNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();


    @Test
    void shouldReturn400ForConstraintViolation() {
        Map<String, List<String>> result = handler.handleConstraintViolation(
                new ConstraintViolationException("ID inválido", Set.of()));
        assertThat(result).containsKey("errors");
        assertThat(result.get("errors")).isInstanceOf(List.class);
    }

    @Test
    void shouldNotExposeMethodNameInConstraintViolationResponse() {
        Map<String, List<String>> result = handler.handleConstraintViolation(
                new ConstraintViolationException("test", Set.of()));
        assertThat(result).doesNotContainKey("error");
    }

    @Test
    void shouldReturn400ForValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "customerId", "não pode ser vazio")));

        Map<String, List<String>> result = handler.handleValidation(ex);

        assertThat(result).containsKey("errors");
        assertThat(result.get("errors")).contains("não pode ser vazio");
    }

    @Test
    void shouldReturn503WithoutKafkaDetails() {
        Map<String, String> result = handler.handlePublishingFailure(
                new EventPublishingException("Kafka offline", null));
        assertThat(result.get("error"))
                .doesNotContain("Kafka")
                .contains("indisponível");
    }

    @Test
    void shouldReturn405WithAllowedMethodsAndDocsLink() {
        Map<String, String> result = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("GET", List.of("POST")));
        assertThat(result)
                .containsKey("error")
                .containsEntry("allowed", "POST")
                .containsEntry("docs", "/swagger-ui.html");
        assertThat(result.get("error")).contains("GET");
    }

    @Test
    void shouldReturn404WithoutExposingResourcePath() {
        Map<String, String> result = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/nao-existe", "/nao-existe"));
        assertThat(result)
                .containsKey("error")
                .containsEntry("docs", "/swagger-ui.html");
        assertThat(result.get("error"))
                .doesNotContain("/nao-existe")
                .contains("não encontrado");
    }

    @Test
    void shouldReturn404WithGenericMessageWithoutExposingOrderId() {
        Map<String, String> result = handler.handleOrderNotFound(new OrderNotFoundException("abc-123"));
        assertThat(result.get("error"))
                .doesNotContain("abc-123")
                .isEqualTo("Pedido não encontrado");
    }

    @Test
    void shouldReturn500WithGenericMessage() {
        Map<String, String> result = handler.handleUnexpected(new RuntimeException("boom"));
        assertThat(result.get("error")).contains("Erro interno");
    }
}
