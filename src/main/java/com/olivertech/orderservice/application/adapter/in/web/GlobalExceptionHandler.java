package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.exception.OrderNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraintViolation(ConstraintViolationException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, List<String>> handleValidation(MethodArgumentNotValidException ex) {
        return Map.of("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).toList());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Map<String, String> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedMethods() != null
                ? String.join(", ", ex.getSupportedMethods())
                : "desconhecido";
        return Map.of(
                "error",   "Método '" + ex.getMethod() + "' não suportado neste endpoint.",
                "allowed", allowed,
                "docs",    "/swagger-ui.html"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoResourceFound(NoResourceFoundException ex) {
        return Map.of(
                "error", "Endpoint não encontrado",
                "docs",  "/swagger-ui.html"
        );
    }

    @ExceptionHandler(EventPublishingException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handlePublishingFailure(EventPublishingException ex) {
        log.error("Falha de publicação Kafka", ex);
        return Map.of("error", "Serviço temporariamente indisponível. Tente novamente.");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleOrderNotFound(OrderNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return Map.of("error", "Erro interno. Tente novamente mais tarde.");
    }
}
