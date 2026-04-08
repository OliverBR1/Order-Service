package com.olivertech.orderservice.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthFilterTest {

    private static final String VALID_KEY = "test-secret-key-12345";

    ApiKeyAuthFilter        filter;
    MockHttpServletRequest  request;
    MockHttpServletResponse response;
    MockFilterChain         chain;

    @BeforeEach
    void setup() {
        filter   = new ApiKeyAuthFilter(VALID_KEY);
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain    = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void shouldAuthenticateWithValidApiKey() throws Exception {
        request.addHeader("X-API-Key", VALID_KEY);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("api-client");
    }

    @Test
    void shouldDelegateToNextFilterWithValidKey() throws Exception {
        request.addHeader("X-API-Key", VALID_KEY);
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }


    @Test
    void shouldNotAuthenticateWithInvalidApiKey() throws Exception {
        request.addHeader("X-API-Key", "wrong-key");
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotAuthenticateWithMissingApiKey() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldStillDelegateToNextFilterWithInvalidKey() throws Exception {
        request.addHeader("X-API-Key", "bad-key");
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldStillDelegateToNextFilterWithNoKey() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldRejectKeyWithCorrectLengthButWrongContent() throws Exception {
        String sameLength = "X".repeat(VALID_KEY.length());
        request.addHeader("X-API-Key", sameLength);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

