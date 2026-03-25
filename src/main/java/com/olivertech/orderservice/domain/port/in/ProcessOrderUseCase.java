package com.olivertech.orderservice.domain.port.in;

/** Porta de entrada: processa um pedido já persistido a partir do seu ID. */
public interface ProcessOrderUseCase {
    void execute(String orderId);
}
