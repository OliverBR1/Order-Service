package com.olivertech.orderservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

    @Test
    void shouldRegisterApiKeySecurityScheme() {

        OpenAPI api = config.orderServiceOpenAPI();
        assertThat(api.getComponents()).isNotNull();
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("X-API-Key");

        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("X-API-Key");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(scheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(scheme.getName()).isEqualTo("X-API-Key");
    }
}
