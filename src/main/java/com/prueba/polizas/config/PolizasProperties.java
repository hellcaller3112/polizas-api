package com.prueba.polizas.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Parámetros de negocio e integración configurables desde application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "polizas")
public record PolizasProperties(
        @NotNull @DecimalMin("0.0") BigDecimal ipc,
        @NotNull Core core
) {
    public record Core(@NotBlank String url, @Positive int timeoutMs) {
    }
}
