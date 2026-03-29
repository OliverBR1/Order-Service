package com.olivertech.orderservice.domain.port.in;

import com.olivertech.orderservice.domain.model.Order;

import java.util.List;

public interface ListOrdersUseCase {
    List<Order> execute();
}

