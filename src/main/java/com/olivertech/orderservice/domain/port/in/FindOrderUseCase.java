package com.olivertech.orderservice.domain.port.in;

import com.olivertech.orderservice.domain.model.Order;

import java.util.Optional;

/** Porta de entrada: busca um pedido pelo seu ID. */
public interface FindOrderUseCase {
    Optional<Order> execute(String orderId);
}

