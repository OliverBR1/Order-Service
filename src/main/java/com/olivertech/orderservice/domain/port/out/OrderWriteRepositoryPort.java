package com.olivertech.orderservice.domain.port.out;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;

public interface OrderWriteRepositoryPort {
    void save(Order order);
    void updateStatus(String id, OrderStatus newStatus);
}

