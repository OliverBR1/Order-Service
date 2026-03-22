package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.application.dto.OrderRequest;
import com.olivertech.orderservice.application.dto.OrderResponse;
import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.CreateOrderUseCase;
import com.olivertech.orderservice.domain.port.in.GetOrderStatusUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    CreateOrderUseCase createUC;
    @Mock
    GetOrderStatusUseCase getStatusUC;
    @InjectMocks
    OrderController controller;

    @Test
    void shouldDelegateCreateToUseCase() {
        OrderRequest  req  = new OrderRequest("c", BigDecimal.TEN);
        OrderResponse resp = new OrderResponse("id-1", OrderStatus.PENDING);
        when(createUC.execute(req)).thenReturn(resp);
        assertThat(controller.createOrder(req)).isEqualTo(resp);
        verifyNoInteractions(getStatusUC);
    }
    @Test void shouldReturn200WhenFound() {
        when(getStatusUC.execute("id-1")).thenReturn(Optional.of(OrderStatus.PROCESSED));
        assertThat(controller.getStatus("id-1").getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test void shouldReturn404WhenNotFound() {
        when(getStatusUC.execute("x")).thenReturn(Optional.empty());
        assertThat(controller.getStatus("x").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test void shouldPropagateUseCaseException() {
        when(createUC.execute(any())).thenThrow(EventPublishingException.class);
        assertThatThrownBy(() -> controller.createOrder(new OrderRequest("c",BigDecimal.TEN)))
                .isInstanceOf(EventPublishingException.class);
    }
}
