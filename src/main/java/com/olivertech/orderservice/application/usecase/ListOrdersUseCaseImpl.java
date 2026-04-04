package com.olivertech.orderservice.application.usecase;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.in.ListOrdersUseCase;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListOrdersUseCaseImpl implements ListOrdersUseCase {

    private final OrderReadRepositoryPort readRepo;

    public ListOrdersUseCaseImpl(OrderReadRepositoryPort readRepo) {
        this.readRepo = readRepo;
    }

    @Override
    public List<Order> execute() {
        return readRepo.findAll();
    }

    @Override
    public List<Order> execute(int page, int size) {
        return readRepo.findAll(page, size);
    }
}

