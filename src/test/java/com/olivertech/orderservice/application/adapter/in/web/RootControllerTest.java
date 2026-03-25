package com.olivertech.orderservice.application.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RootControllerTest {

    RootController controller = new RootController();

    @Test
    void shouldRedirectToSwaggerUi() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);

        controller.redirectToSwagger(response);

        verify(response).sendRedirect("/swagger-ui.html");
    }
}

