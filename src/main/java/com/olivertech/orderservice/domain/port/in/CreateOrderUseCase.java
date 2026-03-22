package com.olivertech.orderservice.domain.port.in;

import com.olivertech.orderservice.application.dto.OrderRequest;
import com.olivertech.orderservice.application.dto.OrderResponse;

public interface CreateOrderUseCase    { OrderResponse execute(OrderRequest request); }