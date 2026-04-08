package com.olivertech.orderservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter baseado em janela fixa (Fixed Window Counter).
 * Implementado sem dependências externas, usando ConcurrentHashMap + AtomicInteger.
 *
 * Roda em @Order(-200), antes do Spring Security (@Order(-100)),
 * para bloquear DoS mesmo de requisições não autenticadas sem custo de autenticação.
 *
 * Limites: 100 requisições / minuto por cliente (API key ou IP de origem).
 */
@Component
@Order(-200)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int  LIMIT_PER_WINDOW  = 100;
    private static final long WINDOW_MILLIS     = 60_000L; // 1 minuto

    /**
     * Bucket: [0] = contagem de requisições, [1] = início da janela em ms (AtomicLong via long em array)
     * Usamos Object[] para poder armazenar tanto o counter quanto o timestamp.
     */
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientId = resolveClientId(request);
        long   now      = System.currentTimeMillis();

        // computeIfAbsent garante criação atômica; synchronized no array para incremento seguro
        long[] window = counters.computeIfAbsent(clientId, k -> new long[]{0L, now});

        boolean allowed;
        synchronized (window) {
            if (now - window[1] >= WINDOW_MILLIS) {
                // Janela expirada: reinicia contador e timestamp
                window[0] = 1L;
                window[1] = now;
                allowed = true;
            } else if (window[0] < LIMIT_PER_WINDOW) {
                window[0]++;
                allowed = true;
            } else {
                allowed = false;
            }
        }

        if (!allowed) {
            log.warn("Rate limit excedido para cliente={}", clientId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":\"Limite de requisições excedido. Tente novamente em breve.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Identifica o cliente pela API Key (se presente) ou pelo IP de origem.
     * Usa apenas o primeiro IP do header X-Forwarded-For para evitar spoofing com lista de IPs.
     */
    private String resolveClientId(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "key:" + apiKey;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
