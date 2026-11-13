package com.prueba.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Contrato del servicio agnóstico de edición del CORE.
 * Ejemplo: {"evento": "ACTUALIZACION", "polizaId": 555}
 */
public record CoreEventoRequest(
        @NotBlank String evento,
        @NotNull Long polizaId
) {
    public static final String ACTUALIZACION = "ACTUALIZACION";

    public static CoreEventoRequest actualizacion(Long polizaId) {
        return new CoreEventoRequest(ACTUALIZACION, polizaId);
    }
}
