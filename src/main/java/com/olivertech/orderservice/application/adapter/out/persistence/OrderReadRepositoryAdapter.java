package com.olivertech.orderservice.application.adapter.out.persistence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAll().stream()
                .map(OrderEntity::toDomain)
                .toList();
    }

    @Override
    public List<Order> findAll(int page, int size) {
        return jpaRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .stream()
                .map(OrderEntity::toDomain)
                .toList();
    }
}
