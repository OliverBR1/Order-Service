package com.olivertech.orderservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    OpenApiConfig config = new OpenApiConfig();

    @Test
    void shouldCreateOpenAPIWithCorrectTitle() {
        OpenAPI api = config.orderServiceOpenAPI();
        assertThat(api.getInfo().getTitle()).isEqualTo("Order Service API");
    }

    @Test
    void shouldCreateOpenAPIWithCorrectVersion() {
        OpenAPI api = config.orderServiceOpenAPI();
        assertThat(api.getInfo().getVersion()).isEqualTo("v1.0.0");
    }

    @Test
    void shouldCreateOpenAPIWithContactInfo() {
        OpenAPI api = config.orderServiceOpenAPI();
        assertThat(api.getInfo().getContact().getName()).isEqualTo("Time de Engenharia");
        assertThat(api.getInfo().getContact().getEmail()).isEqualTo("eng@empresa.com");
    }

    @Test
    void shouldCreateOpenAPIWithDescription() {
        OpenAPI api = config.orderServiceOpenAPI();
        assertThat(api.getInfo().getDescription())
                .isEqualTo("API assíncrona de pedidos com Apache Kafka");
    }
}

