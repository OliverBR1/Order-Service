package com.olivertech.orderservice.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("API assíncrona de pedidos com Apache Kafka")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Time de Engenharia")
                                .email("eng@empresa.com")))
                // CORREÇÃO: sem URL hardcoded — Swagger usa o host corrente
                // automaticamente (funciona em http dev e https prod)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT sem o prefixo 'Bearer '")));
    }
}

