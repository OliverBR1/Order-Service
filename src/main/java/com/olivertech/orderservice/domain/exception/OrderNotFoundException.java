package com.olivertech.orderservice.domain.exception;

import java.io.Serial;

public class OrderNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(String orderId) {
        super("Pedido não encontrado: " + orderId);
    }
}
