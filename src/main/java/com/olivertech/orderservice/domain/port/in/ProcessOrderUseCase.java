package com.olivertech.orderservice.domain.port.in;

public interface ProcessOrderUseCase {
    void execute(String orderId);
}
