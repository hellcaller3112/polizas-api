package com.prueba.polizas.dto;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolizaResponse(
        Long id,
        String numeroPoliza,
        TipoPoliza tipo,
        EstadoPoliza estado,
        String tomador,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int vigenciaMeses,
        BigDecimal canonMensual,
        BigDecimal prima,
        int numeroRenovaciones,
        long riesgosActivos
) {
    public static PolizaResponse from(Poliza poliza) {
        return new PolizaResponse(
                poliza.getId(),
                poliza.getNumeroPoliza(),
                poliza.getTipo(),
                poliza.getEstado(),
                poliza.getTomador(),
                poliza.getFechaInicio(),
                poliza.getFechaFin(),
                poliza.getVigenciaMeses(),
                poliza.getCanonMensual(),
                poliza.getPrima(),
                poliza.getNumeroRenovaciones(),
                poliza.getCantidadRiesgosActivos()
        );
    }
}
