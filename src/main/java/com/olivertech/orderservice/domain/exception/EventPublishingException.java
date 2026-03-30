package com.olivertech.orderservice.domain.exception;

import java.io.Serial;

public class EventPublishingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
