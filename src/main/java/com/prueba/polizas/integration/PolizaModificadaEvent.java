package com.prueba.polizas.integration;

/**
 * Evento de dominio publicado cada vez que una operación modifica el estado de una póliza o de sus riesgos.
 *
 * @param polizaId  póliza afectada
 * @param operacion operación que originó el cambio (RENOVACION, CANCELACION_POLIZA, ...)
 */
public record PolizaModificadaEvent(Long polizaId, String operacion) {
}
