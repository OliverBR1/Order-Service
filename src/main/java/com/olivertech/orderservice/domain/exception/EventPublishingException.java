package com.olivertech.orderservice.domain.exception;

public class EventPublishingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
