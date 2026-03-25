package com.olivertech.orderservice.application.usecase;

import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.port.in.CreateOrderUseCase;
import com.olivertech.orderservice.domain.port.out.OrderEventPublisherPort;
import com.olivertech.orderservice.domain.port.out.OrderWriteRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public Order execute(String customerId, BigDecimal amount) {
        Order order = Order.create(customerId, amount);
        writeRepo.save(order);

        try {
            // O adaptador Kafka faz o mapeamento Order → OrderEvent internamente
            publisher.publish(order).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();  // restore interrupted status
            throw new EventPublishingException(
                    "Falha ao publicar evento para orderId=" + order.getId(), ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new EventPublishingException(
                    "Falha ao publicar evento para orderId=" + order.getId(), ex);
        }

        log.info("Pedido criado: orderId={}", order.getId());
        return order;
    }
}
