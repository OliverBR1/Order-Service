package com.olivertech.orderservice.application.usecase;

import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.GetOrderStatusUseCase;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetOrderStatusUseCaseImpl implements GetOrderStatusUseCase {

    private final OrderReadRepositoryPort readRepo;

    public GetOrderStatusUseCaseImpl(OrderReadRepositoryPort readRepo) {
        this.readRepo = readRepo;
    }

    @Override
    public Optional<OrderStatus> execute(String orderId) {
        return readRepo.findById(orderId)
                .map(Order::getStatus);
    }
}
