package com.olivertech.orderservice.domain.port.in;

import com.olivertech.orderservice.domain.model.Order;

public interface FindOrderUseCase {
    java.util.Optional<Order> execute(String orderId);
}
