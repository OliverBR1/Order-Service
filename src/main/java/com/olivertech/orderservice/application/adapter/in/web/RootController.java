package com.olivertech.orderservice.application.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Redireciona a raiz da aplicação para o Swagger UI.
 * Evita o erro 404/405 quando o browser acessa http://localhost:8081.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public void redirectToSwagger(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui.html");
    }
}

