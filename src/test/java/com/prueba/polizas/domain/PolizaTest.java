package com.prueba.polizas.domain;

import com.prueba.polizas.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de las reglas de negocio del agregado Póliza (sin Spring ni base de datos).
 */
class PolizaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);

    private Poliza individual() {
        Poliza poliza = Poliza.crear("IND-1", TipoPoliza.INDIVIDUAL, "Tomador", INICIO, 12);
        poliza.agregarRiesgo("Calle 1", "Arrendatario", "Arrendador", new BigDecimal("1000000"));
        return poliza;
    }

    private Poliza colectiva() {
        Poliza poliza = Poliza.crear("COL-1", TipoPoliza.COLECTIVA, "Inmobiliaria", INICIO, 12);
        poliza.agregarRiesgo("Calle 1", "A1", "B1", new BigDecimal("1000000"));
        poliza.agregarRiesgo("Calle 2", "A2", "B2", new BigDecimal("2000000"));
        return poliza;
    }

    @Test
    @DisplayName("La prima es el canon mensual por los meses de vigencia")
    void primaEsCanonPorMeses() {
        Poliza poliza = individual();

        assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1000000");
        assertThat(poliza.getPrima()).isEqualByComparingTo("12000000");
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
    }

    @Test
    @DisplayName("En una colectiva la prima es la suma de las primas de sus riesgos")
    void primaColectivaEsSumaDeRiesgos() {
        Poliza poliza = colectiva();

        assertThat(poliza.getCanonMensual()).isEqualByComparingTo("3000000");
        assertThat(poliza.getPrima()).isEqualByComparingTo("36000000");
    }

    @Test
    @DisplayName("Renovar incrementa canon y prima según el IPC, conserva la vigencia y pasa a RENOVADA")
    void renovarAplicaIpc() {
        Poliza poliza = individual();

        poliza.renovar(new BigDecimal("5"));

        assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1050000");
        assertThat(poliza.getPrima()).isEqualByComparingTo("12600000");
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(poliza.getVigenciaMeses()).isEqualTo(12);
        assertThat(poliza.getFechaInicio()).isEqualTo(INICIO.plusMonths(12));
        assertThat(poliza.getFechaFin()).isEqualTo(INICIO.plusMonths(24));
        assertThat(poliza.getNumeroRenovaciones()).isEqualTo(1);
    }

    @Test
    @DisplayName("No se puede renovar una póliza cancelada")
    void noRenovarCancelada() {
        Poliza poliza = individual();
        poliza.cancelar();

        assertThatThrownBy(() -> poliza.renovar(new BigDecimal("5")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se puede renovar una póliza cancelada");
    }

    @Test
    @DisplayName("Cancelar la póliza cancela todos sus riesgos")
    void cancelarPolizaCancelaRiesgos() {
        Poliza poliza = colectiva();

        poliza.cancelar();

        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(poliza.getRiesgos()).allMatch(r -> r.getEstado() == EstadoRiesgo.CANCELADO);
    }

    @Test
    @DisplayName("Una póliza individual solo puede tener 1 riesgo")
    void individualSoloUnRiesgo() {
        Poliza poliza = individual();

        assertThatThrownBy(() -> poliza.agregarRiesgo("Calle 2", "X", "Y", new BigDecimal("500000")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("solo puede tener 1 riesgo");
    }

    @Test
    @DisplayName("Cancelar un riesgo de una colectiva recalcula la prima de la póliza")
    void cancelarRiesgoRecalculaPrima() {
        Poliza poliza = colectiva();
        Riesgo riesgo = poliza.getRiesgos().get(1); // canon 2.000.000

        poliza.cancelarRiesgo(riesgo);

        assertThat(riesgo.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
        assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1000000");
        assertThat(poliza.getPrima()).isEqualByComparingTo("12000000");
    }

    @Test
    @DisplayName("No se agregan riesgos a una póliza cancelada")
    void noAgregarRiesgoAPolizaCancelada() {
        Poliza poliza = colectiva();
        poliza.cancelar();

        assertThatThrownBy(() -> poliza.agregarRiesgo("Calle 3", "X", "Y", new BigDecimal("500000")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("El canon debe ser mayor a cero")
    void canonDebeSerPositivo() {
        Poliza poliza = Poliza.crear("COL-2", TipoPoliza.COLECTIVA, "Inmobiliaria", INICIO, 12);

        assertThatThrownBy(() -> poliza.agregarRiesgo("Calle 1", "X", "Y", BigDecimal.ZERO))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
