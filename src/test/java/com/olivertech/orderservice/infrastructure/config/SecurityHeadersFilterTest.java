package com.olivertech.orderservice.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersFilterTest {

    private static final String TEST_CSP = "default-src 'none'; frame-ancestors 'none'";

    SecurityHeadersFilter   filter;
    MockHttpServletRequest  request;
    MockHttpServletResponse response;
    MockFilterChain         chain;

    @BeforeEach
    void setup() {
        filter = new SecurityHeadersFilter();
        ReflectionTestUtils.setField(filter, "contentSecurityPolicy", TEST_CSP);
        ReflectionTestUtils.setField(filter, "hstsEnabled", false); // padrão dev = false
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
    void shouldSetContentSecurityPolicyFromConfig() throws Exception {
        // M2 FIX: CSP é configurável por ambiente via @Value
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo(TEST_CSP);
    }

    @Test
    void shouldNotSetHstsWhenDisabled() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void shouldSetHstsWhenEnabled() throws Exception {
        ReflectionTestUtils.setField(filter, "hstsEnabled", true);
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains; preload");
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
    void shouldSuppressTechnologyDisclosureHeaders() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("X-Powered-By")).isEmpty();
        assertThat(response.getHeader("Server")).isEmpty();
    }

    @Test
    void shouldDelegateToNextFilter() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }
}

