package com.prueba.polizas.integration;

import com.prueba.polizas.config.ApiKeyFilter;
import com.prueba.polizas.config.PolizasProperties;
import com.prueba.polizas.dto.CoreEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador HTTP hacia el servicio agnóstico de edición del CORE.
 * <p>
 * Si el CORE no responde, la operación de negocio no se revierte: se registra el error.
 * En producción el evento quedaría en una tabla outbox para reintentos (ver diseño, Módulo 1).
 */
@Component
public class RestCoreClient implements CoreClient {

    private static final Logger log = LoggerFactory.getLogger(RestCoreClient.class);

    private final RestClient restClient;
    private final String url;

    public RestCoreClient(RestClient.Builder builder, PolizasProperties propiedades,
                          @Value("${security.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(propiedades.core().timeoutMs());
        requestFactory.setReadTimeout(propiedades.core().timeoutMs());

        this.url = propiedades.core().url();
        this.restClient = builder
                .requestFactory(requestFactory)
                .defaultHeader(ApiKeyFilter.HEADER, apiKey)
                .build();
    }

    @Override
    public void enviarActualizacion(Long polizaId, String operacion) {
        CoreEventoRequest evento = CoreEventoRequest.actualizacion(polizaId);
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Actualización enviada al CORE: polizaId={}, operacion={}", polizaId, operacion);
        } catch (RestClientException e) {
            log.error("No fue posible enviar la actualización al CORE: polizaId={}, operacion={}. Causa: {}",
                    polizaId, operacion, e.getMessage());
        }
    }
}
