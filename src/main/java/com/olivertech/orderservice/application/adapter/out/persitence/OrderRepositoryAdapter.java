package com.olivertech.orderservice.application.adapter.out.persitence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderRepositoryAdapter
        implements OrderWriteRepositoryPort, OrderReadRepositoryPort {

    private final JpaOrderRepository jpaRepository;

    public OrderRepositoryAdapter(JpaOrderRepository jpaRepository) {
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