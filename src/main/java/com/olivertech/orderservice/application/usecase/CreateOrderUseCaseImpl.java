package com.olivertech.orderservice.application.usecase;

import com.olivertech.orderservice.application.dto.OrderEvent;
import com.olivertech.orderservice.application.dto.OrderRequest;
import com.olivertech.orderservice.application.dto.OrderResponse;
import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.in.CreateOrderUseCase;
import com.olivertech.orderservice.domain.port.out.OrderEventPublisherPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateOrderUseCaseImpl.class);

    private final OrderWriteRepositoryPort writeRepo;
    private final OrderEventPublisherPort  publisher;

    public CreateOrderUseCaseImpl(OrderWriteRepositoryPort writeRepo,
                                  OrderEventPublisherPort publisher) {
        this.writeRepo = writeRepo;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public OrderResponse execute(OrderRequest request) {
        Order order = Order.create(request.customerId(), request.amount());
        writeRepo.save(order);

        try {
            // CORREÇÃO: bloqueia até confirmação do broker.
            // Se Kafka falhar, EventPublishingException desfaz a transação.
            // Para consistência total em produção: use Transactional Outbox Pattern.
            publisher.publish(OrderEvent.from(order)).get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException(
                    "Falha ao publicar evento para orderId=" + order.getId(), ex);
        }

        log.info("Pedido criado: orderId={}", order.getId());
        return OrderResponse.from(order);
    }
}
