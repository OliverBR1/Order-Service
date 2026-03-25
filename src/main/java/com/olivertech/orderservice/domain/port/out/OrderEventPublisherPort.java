package com.olivertech.orderservice.domain.port.out;

import com.olivertech.orderservice.domain.model.Order;

import java.util.concurrent.CompletableFuture;

/** Porta de saída: publica um evento de domínio para o broker de mensagens. */
public interface OrderEventPublisherPort {
    CompletableFuture<Void> publish(Order order);
}
