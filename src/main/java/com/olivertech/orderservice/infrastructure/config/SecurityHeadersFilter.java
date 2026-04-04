package com.olivertech.orderservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adiciona headers de segurança HTTP em todas as respostas.
 * Mitiga: Clickjacking, MIME sniffing, XSS, downgrade HTTP→HTTPS.
 */
@Component
@Order(1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,   // NOSONAR — override de método não-nulo
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Impede que a página seja carregada em iframes (Clickjacking)
        response.setHeader("X-Frame-Options", "DENY");

        // Impede que o browser "adivinhe" o Content-Type (MIME sniffing)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Força HTTPS por 1 ano, incluindo subdomínios
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // Política de conteúdo: como é uma API JSON pura, bloqueia tudo exceto same-origin
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'");

        // Remove informações do servidor da resposta
        response.setHeader("X-Powered-By", "");
        response.setHeader("Server", "");

        // Controla o que é enviado no header Referer
        response.setHeader("Referrer-Policy", "no-referrer");

        // Desativa recursos do browser que não são necessários em uma API
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // Impede cache de respostas que contêm dados sensíveis
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}





