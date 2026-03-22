package com.olivertech.orderservice.domain.port.in;

import com.olivertech.orderservice.application.dto.OrderEvent;

public interface ProcessOrderUseCase   { void execute(OrderEvent event); }
