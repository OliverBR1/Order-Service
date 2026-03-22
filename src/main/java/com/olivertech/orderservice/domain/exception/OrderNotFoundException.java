package com.olivertech.orderservice.domain.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) {
        super("Pedido não encontrado: " + orderId);
    }
}
