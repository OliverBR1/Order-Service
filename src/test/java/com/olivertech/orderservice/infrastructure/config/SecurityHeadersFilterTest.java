package com.olivertech.orderservice.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersFilterTest {

    SecurityHeadersFilter filter;
    MockHttpServletRequest  request;
    MockHttpServletResponse response;
    MockFilterChain         chain;

    @BeforeEach
    void setup() {
        filter   = new SecurityHeadersFilter();
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain    = new MockFilterChain();
    }

    @Test
    void shouldSetXFrameOptionsDeny() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void shouldSetXContentTypeOptionsNoSniff() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void shouldSetStrictTransportSecurity() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    void shouldSetContentSecurityPolicy() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo("default-src 'none'; frame-ancestors 'none'");
    }

    @Test
    void shouldSetReferrerPolicy() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    }

    @Test
    void shouldSetPermissionsPolicy() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Permissions-Policy"))
                .isEqualTo("camera=(), microphone=(), geolocation=(), payment=()");
    }

    @Test
    void shouldSetCacheControlNoStore() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    }

    @Test
    void shouldDelegateToNextFilter() throws Exception {
        filter.doFilter(request, response, chain);
        // MockFilterChain records the last request/response passed through
        assertThat(chain.getRequest()).isNotNull();
    }
}

