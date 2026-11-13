package com.prueba.polizas.exception;

/**
 * Se lanza cuando una operación incumple una regla de negocio (HTTP 422).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
