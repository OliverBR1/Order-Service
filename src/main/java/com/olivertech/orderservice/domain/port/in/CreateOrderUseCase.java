package com.olivertech.orderservice.domain.port.in;

import java.math.BigDecimal;

import com.olivertech.orderservice.domain.model.Order;

/** Porta de entrada: cria um novo pedido e o persiste. */
public interface CreateOrderUseCase {
    Order execute(String customerId, BigDecimal amount);
}
