package com.olivertech.orderservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Valida o header {@code X-API-Key} em tempo constante (resistente a timing attacks).
 * Se a chave for válida, autentica o cliente no SecurityContext do Spring Security.
 * Não é anotado como {@code @Component} para evitar registro duplo como Servlet filter.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final byte[] validKeyBytes;

    public ApiKeyAuthFilter(String validApiKey) {
        this.validKeyBytes = validApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null
                && MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), validKeyBytes)) {
            var auth = new UsernamePasswordAuthenticationToken("api-client", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}

