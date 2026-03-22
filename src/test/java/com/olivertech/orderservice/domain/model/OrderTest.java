package com.olivertech.orderservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Order — Entidade de Domínio")
class OrderTest {

    @Nested
    @DisplayName("Order.create()")
    class Create {
        @Test
        void shouldCreateWithPendingStatus() {
            Order o = Order.create("cust-1", new BigDecimal("99.90"));
            assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(o.getId()).isNotBlank();
            assertThat(o.getCustomerId()).isEqualTo("cust-1");
        }
        @Test void shouldGenerateUniqueIds() {
            assertThat(Order.create("c",BigDecimal.ONE).getId())
                    .isNotEqualTo(Order.create("c",BigDecimal.ONE).getId());
        }
        @Test void shouldRejectNullCustomerId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Order.create(null, BigDecimal.TEN));
        }
        @Test void shouldRejectZeroAmount() {
            assertThatThrownBy(() -> Order.create("c", BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        @Test void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Order.create("c", new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested @DisplayName("Order.markAsProcessed()")
    class MarkAsProcessed {
        @Test void shouldTransitionToProcessed() {
            Order o = Order.create("c", BigDecimal.TEN);
            o.markAsProcessed();
            assertThat(o.getStatus()).isEqualTo(OrderStatus.PROCESSED);
        }
        @Test void shouldRejectAlreadyProcessed() {
            Order o = Order.create("c", BigDecimal.TEN);
            o.markAsProcessed();
            assertThatThrownBy(o::markAsProcessed)
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
