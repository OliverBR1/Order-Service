package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de escrita do repositório de pedidos (SRP: apenas escrita).
 * Implementa somente OrderWriteRepositoryPort.
 */
@Component
public class OrderWriteRepositoryAdapter implements OrderWriteRepositoryPort {

    private final JpaOrderRepository jpaRepository;

    public OrderWriteRepositoryAdapter(JpaOrderRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Order order) {
        jpaRepository.save(OrderEntity.from(order));
    }

    @Override
    public void updateStatus(String id, OrderStatus newStatus) {
        jpaRepository.updateStatus(id, newStatus.name());
    }
}

