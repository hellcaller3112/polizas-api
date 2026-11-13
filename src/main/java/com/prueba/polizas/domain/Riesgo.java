package com.prueba.polizas.domain;

import com.prueba.polizas.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Un riesgo es cada contrato de arrendamiento cubierto: inmueble + arrendatario (asegurado)
 * + arrendador (beneficiario) + canon mensual.
 * <p>
 * Sus cambios de estado se hacen siempre a través de {@link Poliza}, que es el agregado raíz.
 */
@Entity
@Table(name = "riesgo")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(nullable = false, length = 200)
    private String direccionInmueble;

    /** Asegurado. */
    @Column(nullable = false, length = 150)
    private String arrendatario;

    /** Beneficiario. */
    @Column(nullable = false, length = 150)
    private String arrendador;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado;

    @Column(nullable = false)
    private LocalDate fechaInclusion;

    private LocalDate fechaCancelacion;

    protected Riesgo() {
        // requerido por JPA
    }

    Riesgo(Poliza poliza, String direccionInmueble, String arrendatario, String arrendador,
           BigDecimal canonMensual, int vigenciaMeses) {
        if (canonMensual == null || canonMensual.signum() <= 0) {
            throw new ReglaNegocioException("El canon mensual debe ser mayor a 0");
        }
        this.poliza = poliza;
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
        this.arrendador = arrendador;
        this.canonMensual = canonMensual.setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima(vigenciaMeses);
        this.estado = EstadoRiesgo.ACTIVO;
        this.fechaInclusion = LocalDate.now();
    }

    void incrementarCanon(BigDecimal factor, int vigenciaMeses) {
        this.canonMensual = canonMensual.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima(vigenciaMeses);
    }

    void cancelar() {
        if (estado == EstadoRiesgo.CANCELADO) {
            throw new ReglaNegocioException("El riesgo ya se encuentra cancelado");
        }
        this.estado = EstadoRiesgo.CANCELADO;
        this.fechaCancelacion = LocalDate.now();
    }

    boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    private BigDecimal calcularPrima(int vigenciaMeses) {
        return canonMensual.multiply(BigDecimal.valueOf(vigenciaMeses)).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Poliza getPoliza() {
        return poliza;
    }

    public String getDireccionInmueble() {
        return direccionInmueble;
    }

    public String getArrendatario() {
        return arrendatario;
    }

    public String getArrendador() {
        return arrendador;
    }

    public BigDecimal getCanonMensual() {
        return canonMensual;
    }

    public BigDecimal getPrima() {
        return prima;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }

    public LocalDate getFechaInclusion() {
        return fechaInclusion;
    }

    public LocalDate getFechaCancelacion() {
        return fechaCancelacion;
    }
}
