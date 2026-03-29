package com.olivertech.orderservice.domain.port.out;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderReadRepositoryPort {
    Optional<Order> findById(String id);
    boolean existsByIdAndStatus(String id, OrderStatus status);
    List<Order> findAll();
}

