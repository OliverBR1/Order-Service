package com.olivertech.orderservice.application.usecase;

import com.olivertech.orderservice.domain.exception.OrderNotFoundException;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.ProcessOrderUseCase;
import com.olivertech.orderservice.domain.port.out.OrderReadRepositoryPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessOrderUseCaseImpl implements ProcessOrderUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(ProcessOrderUseCaseImpl.class);

    private final OrderReadRepositoryPort readRepo;
    private final OrderWriteRepositoryPort writeRepo;

    public ProcessOrderUseCaseImpl(OrderReadRepositoryPort readRepo,
                                   OrderWriteRepositoryPort writeRepo) {
        this.readRepo = readRepo;
        this.writeRepo = writeRepo;
    }

    @Override
    @Transactional
    public void execute(String orderId) {
        if (readRepo.existsByIdAndStatus(orderId, OrderStatus.PROCESSED)) {
            log.warn("Evento duplicado ignorado: orderId={}", orderId);
            return;
        }
        Order order = readRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markAsProcessed();
        writeRepo.updateStatus(order.getId(), order.getStatus());
        log.info("Pedido processado: orderId={}", order.getId());
    }
}
