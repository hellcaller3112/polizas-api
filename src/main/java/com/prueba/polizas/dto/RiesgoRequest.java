package com.prueba.polizas.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RiesgoRequest(
        @NotBlank(message = "La dirección del inmueble es obligatoria")
        @Size(max = 200)
        String direccionInmueble,

        @NotBlank(message = "El arrendatario (asegurado) es obligatorio")
        @Size(max = 150)
        String arrendatario,

        @NotBlank(message = "El arrendador (beneficiario) es obligatorio")
        @Size(max = 150)
        String arrendador,

        @NotNull(message = "El canon mensual es obligatorio")
        @Positive(message = "El canon mensual debe ser mayor a 0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal canonMensual
) {
}
