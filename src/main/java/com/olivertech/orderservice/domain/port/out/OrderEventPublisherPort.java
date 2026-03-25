package com.olivertech.orderservice.domain.port.out;

import com.olivertech.orderservice.domain.model.Order;

import java.util.concurrent.CompletableFuture;

public interface OrderEventPublisherPort {
    CompletableFuture<Void> publish(Order order);
}
