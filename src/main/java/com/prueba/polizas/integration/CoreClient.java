package com.prueba.polizas.integration;

/**
 * Puerto de salida hacia el CORE de seguros (servicio agnóstico de edición en la capa media).
 * El dominio y los servicios dependen de esta interfaz, no de la tecnología de integración.
 */
public interface CoreClient {

    void enviarActualizacion(Long polizaId, String operacion);
}
