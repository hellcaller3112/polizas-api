package com.prueba.polizas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Seguridad mínima: toda petición a la API debe incluir el header {@code x-api-key} con el valor configurado.
 * Swagger y la consola H2 quedan excluidos para facilitar las pruebas locales.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "x-api-key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final List<String> RUTAS_PUBLICAS = List.of("/swagger-ui", "/v3/api-docs", "/h2-console");

    private final byte[] apiKeyEsperada;

    public ApiKeyFilter(@Value("${security.api-key}") String apiKey) {
        this.apiKeyEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey == null || !esValida(apiKey)) {
            log.warn("Acceso denegado a {} {}: x-api-key ausente o inválida", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"title":"No autorizado","status":401,"detail":"Header x-api-key ausente o inválido"}""");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Comparación en tiempo constante para no filtrar información por tiempos de respuesta. */
    private boolean esValida(String apiKey) {
        return MessageDigest.isEqual(apiKeyEsperada, apiKey.getBytes(StandardCharsets.UTF_8));
    }
}
