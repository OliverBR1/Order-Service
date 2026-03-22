package com.olivertech.orderservice.application.adapter.out.persitence;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Component
public class OrderRepositoryAdapter
        implements OrderWriteRepositoryPort, OrderReadRepositoryPort {

    // JpaOrderRepository é uma interface Spring Data JPA — detalhe de infraestrutura
    private final JpaOrderRepository jpaRepository;

    public OrderRepositoryAdapter(JpaOrderRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // ── OrderWriteRepositoryPort ──────────────────────────────────────────────

    @Override
    public void save(Order order) {
        jpaRepository.save(OrderEntity.from(order));
    }

    @Override
    public void updateStatus(String id, OrderStatus newStatus) {
        jpaRepository.updateStatus(id, newStatus.name());
    }

    // ── OrderReadRepositoryPort ───────────────────────────────────────────────

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

// ── JpaOrderRepository — interface Spring Data (detalhe de infra) ─────────────
public interface JpaOrderRepository extends JpaRepository<OrderEntity, String> {

    @Modifying
    @Query("UPDATE OrderEntity o SET o.status = :status WHERE o.id = :id")
    void updateStatus(@Param("id") String id, @Param("status") String status);

    boolean existsByIdAndStatus(String id, String status);
}

// ── OrderEntity — entidade JPA (nunca exposta fora do adapter) ────────────────
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    protected OrderEntity() {}  // JPA requer construtor padrão

    // Factory — converte domínio → entidade JPA
    public static OrderEntity from(Order order) {
        OrderEntity e = new OrderEntity();
        e.id         = order.getId();
        e.customerId = order.getCustomerId();
        e.amount     = order.getAmount();
        e.status     = order.getStatus().name();
        e.createdAt  = order.getCreatedAt();
        return e;
    }

    // Converte entidade JPA → domínio
    public Order toDomain() {
        return Order.reconstitute(id, customerId, amount,
                OrderStatus.valueOf(status), createdAt);
    }
}
