package com.olivertech.orderservice.application.adapter.in.web;

import com.olivertech.orderservice.application.dto.OrderRequest;
import com.olivertech.orderservice.application.dto.OrderResponse;
import com.olivertech.orderservice.domain.exception.EventPublishingException;
import com.olivertech.orderservice.domain.model.Order;
import com.olivertech.orderservice.domain.model.OrderStatus;
import com.olivertech.orderservice.domain.port.in.CreateOrderUseCase;
import com.olivertech.orderservice.domain.port.in.FindOrderUseCase;
import com.olivertech.orderservice.domain.port.in.GetOrderStatusUseCase;
import com.olivertech.orderservice.domain.port.in.ListOrdersUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock CreateOrderUseCase    createUC;
    @Mock FindOrderUseCase      findUC;
    @Mock GetOrderStatusUseCase getStatusUC;
    @Mock ListOrdersUseCase     listOrdersUC;
    @InjectMocks OrderController controller;

    @Test
    void shouldDelegateCreateToUseCase() {
        Order order = Order.create("c", BigDecimal.TEN);
        when(createUC.execute(anyString(), any(BigDecimal.class))).thenReturn(order);

        OrderResponse resp = controller.createOrder(new OrderRequest("c", BigDecimal.TEN));

        assertThat(resp.orderId()).isEqualTo(order.getId());
        assertThat(resp.status()).isEqualTo(OrderStatus.PENDING);
        verifyNoInteractions(getStatusUC, findUC);
    }

    @Test
    void shouldReturn200WhenOrderFound() {
        Order order = Order.create("c", BigDecimal.TEN);
        when(findUC.execute(order.getId())).thenReturn(Optional.of(order));

        ResponseEntity<OrderResponse> response = controller.getOrder(order.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isEqualTo(order.getId());
        assertThat(response.getBody().customerId()).isEqualTo("c");
        assertThat(response.getBody().amount()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        when(findUC.execute("unknown")).thenReturn(Optional.empty());
        assertThat(controller.getOrder("unknown").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn200StatusWhenFound() {
        when(getStatusUC.execute("id-1")).thenReturn(Optional.of(OrderStatus.PROCESSED));
        assertThat(controller.getStatus("id-1").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturn404StatusWhenNotFound() {
        when(getStatusUC.execute("x")).thenReturn(Optional.empty());
        assertThat(controller.getStatus("x").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnAllOrdersAsList() {
        Order o1 = Order.create("c1", BigDecimal.TEN);
        Order o2 = Order.create("c2", new BigDecimal("50.00"));
        when(listOrdersUC.execute(0, 20)).thenReturn(List.of(o1, o2));

        List<OrderResponse> result = controller.listOrders(0, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).customerId()).isEqualTo("c1");
        assertThat(result.get(1).customerId()).isEqualTo("c2");
    }

    @Test
    void shouldReturnEmptyListWhenNoOrders() {
        when(listOrdersUC.execute(0, 20)).thenReturn(List.of());

        List<OrderResponse> result = controller.listOrders(0, 20);

        assertThat(result).isEmpty();
        verifyNoInteractions(createUC, findUC, getStatusUC);
    }

    @Test
    void shouldPropagateUseCaseException() {
        when(createUC.execute(anyString(), any(BigDecimal.class)))
                .thenThrow(EventPublishingException.class);
        assertThatThrownBy(() -> controller.createOrder(new OrderRequest("c", BigDecimal.TEN)))
                .isInstanceOf(EventPublishingException.class);
    }
}
