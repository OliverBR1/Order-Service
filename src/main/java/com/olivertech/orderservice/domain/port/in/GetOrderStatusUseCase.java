package com.olivertech.orderservice.domain.port.in;

import com.olivertech.orderservice.domain.model.OrderStatus;

import java.util.Optional;

public interface GetOrderStatusUseCase { Optional<OrderStatus> execute(String orderId); }
