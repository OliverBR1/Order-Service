package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de leitura do repositório de pedidos (SRP: apenas leitura).
 * Implementa somente OrderReadRepositoryPort.
 */
@Component
public class OrderReadRepositoryAdapter implements OrderReadRepositoryPort {

    private final JpaOrderRepository jpaRepository;

    public OrderReadRepositoryAdapter(JpaOrderRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Order> findById(String id) {
        return jpaRepository.findById(id)
                .map(OrderEntity::toDomain);
    }

    @Override
    public boolean existsByIdAndStatus(String id, OrderStatus status) {
        return jpaRepository.existsByIdAndStatus(id, status.name());
    }
}

