package com.olivertech.orderservice.domain.port.out;

import com.olivertech.orderservice.application.dto.OrderEvent;

import java.util.concurrent.CompletableFuture;

public interface OrderEventPublisherPort {
    CompletableFuture<Void> publish(OrderEvent event);
}

