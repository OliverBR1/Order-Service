package com.olivertech.orderservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Injeta security headers em TODAS as respostas HTTP.
 * Executado com HIGHEST_PRECEDENCE para garantir que mesmo respostas
 * rejeitadas pelo Spring Security (401/403) e pelo RateLimitFilter (429)
 * recebam os headers de segurança.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /** CSP configurável por ambiente via application.properties */
    @Value("${security.headers.csp}")
    private String contentSecurityPolicy;

    /** HSTS deve ser habilitado apenas quando TLS estiver ativo (produção) */
    @Value("${security.headers.hsts.enabled:false}")
    private boolean hstsEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader("X-Frame-Options",        "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        response.setHeader("Referrer-Policy",        "no-referrer");
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma",        "no-cache");

        // Remove headers que revelam tecnologia usada
        response.setHeader("X-Powered-By", "");
        response.setHeader("Server",       "");

        // HSTS só faz sentido com HTTPS — habilitado apenas em produção
        if (hstsEnabled) {
            response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
        }

        filterChain.doFilter(request, response);
    }
}
