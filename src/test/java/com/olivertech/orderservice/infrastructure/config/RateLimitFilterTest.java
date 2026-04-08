package com.olivertech.orderservice.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    RateLimitFilter         filter;
    MockHttpServletRequest  request;
    MockHttpServletResponse response;
    MockFilterChain         chain;

    @BeforeEach
    void setup() {
        filter   = new RateLimitFilter();
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain    = new MockFilterChain();
    }

    @Test
    void shouldAllowFirstRequestWithApiKey() throws Exception {
        request.addHeader("X-API-Key", "test-key");
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldAllowFirstRequestWithIpOnly() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldBlockRequestWhenLimitExceeded() throws Exception {
        request.addHeader("X-API-Key", "blocked-key");

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, long[]> counters =
                (ConcurrentHashMap<String, long[]>) ReflectionTestUtils.getField(filter, "counters");
        counters.put("key:blocked-key", new long[]{100L, System.currentTimeMillis()});

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("Limite de requisições excedido");
    }

    @Test
    void shouldNotDelegateToNextFilterWhenLimitExceeded() throws Exception {
        request.addHeader("X-API-Key", "throttled-key");

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, long[]> counters =
                (ConcurrentHashMap<String, long[]>) ReflectionTestUtils.getField(filter, "counters");
        counters.put("key:throttled-key", new long[]{100L, System.currentTimeMillis()});

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
    }


    @Test
    void shouldResetCounterWhenWindowExpires() throws Exception {
        request.addHeader("X-API-Key", "expiring-key");

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, long[]> counters =
                (ConcurrentHashMap<String, long[]>) ReflectionTestUtils.getField(filter, "counters");
        long expiredStart = System.currentTimeMillis() - 70_000L;
        counters.put("key:expiring-key", new long[]{100L, expiredStart});

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldPreferApiKeyOverIpForClientIdentification() throws Exception {
        request.addHeader("X-API-Key", "preferred-key");
        request.addHeader("X-Forwarded-For", "10.0.0.1");

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, long[]> counters =
                (ConcurrentHashMap<String, long[]>) ReflectionTestUtils.getField(filter, "counters");
        counters.put("key:preferred-key", new long[]{100L, System.currentTimeMillis()});

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void shouldUseFirstIpFromXForwardedForHeader() throws Exception {
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1, 172.16.0.1");

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, long[]> counters =
                (ConcurrentHashMap<String, long[]>) ReflectionTestUtils.getField(filter, "counters");
        counters.put("ip:192.168.1.1", new long[]{100L, System.currentTimeMillis()});

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}

