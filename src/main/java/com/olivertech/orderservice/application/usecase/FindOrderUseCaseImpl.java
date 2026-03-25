package com.olivertech.orderservice.application.usecase;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.in.FindOrderUseCase;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FindOrderUseCaseImpl implements FindOrderUseCase {

    private final OrderReadRepositoryPort readRepo;

    public FindOrderUseCaseImpl(OrderReadRepositoryPort readRepo) {
        this.readRepo = readRepo;
    }

    @Override
    public Optional<Order> execute(String orderId) {
        return readRepo.findById(orderId);
    }
}

