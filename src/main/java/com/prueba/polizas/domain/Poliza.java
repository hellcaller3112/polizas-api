package com.prueba.polizas.domain;

import com.prueba.polizas.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregado raíz. Concentra las reglas de negocio de la póliza y de sus riesgos,
 * de modo que ninguna capa externa pueda dejarla en un estado inválido.
 * <p>
 * El canon vive en cada {@link Riesgo}; la póliza expone el canon y la prima
 * totales calculados a partir de sus riesgos activos.
 */
@Entity
@Table(name = "poliza", indexes = @Index(name = "idx_poliza_tipo_estado", columnList = "tipo, estado"))
public class Poliza {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroPoliza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado;

    @Column(nullable = false, length = 150)
    private String tomador;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private int vigenciaMeses;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima = BigDecimal.ZERO;

    @Column(nullable = false)
    private int numeroRenovaciones;

    private LocalDate fechaCancelacion;

    /** Bloqueo optimista: evita que dos operaciones concurrentes se pisen. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() {
        // requerido por JPA
    }

    private Poliza(String numeroPoliza, TipoPoliza tipo, String tomador, LocalDate fechaInicio, int vigenciaMeses) {
        if (vigenciaMeses <= 0) {
            throw new ReglaNegocioException("La vigencia debe ser de al menos 1 mes");
        }
        this.numeroPoliza = numeroPoliza;
        this.tipo = tipo;
        this.tomador = tomador;
        this.fechaInicio = fechaInicio;
        this.vigenciaMeses = vigenciaMeses;
        this.fechaFin = fechaInicio.plusMonths(vigenciaMeses);
        this.estado = EstadoPoliza.ACTIVA;
    }

    public static Poliza crear(String numeroPoliza, TipoPoliza tipo, String tomador,
                               LocalDate fechaInicio, int vigenciaMeses) {
        return new Poliza(numeroPoliza, tipo, tomador, fechaInicio, vigenciaMeses);
    }

    // ------------------------------------------------------------------ reglas de negocio

    /**
     * Agrega un riesgo a la póliza.
     * Regla: una póliza individual solo puede tener 1 riesgo; no se agregan riesgos a pólizas canceladas.
     */
    public Riesgo agregarRiesgo(String direccionInmueble, String arrendatario, String arrendador,
                                BigDecimal canonMensual) {
        validarNoCancelada("No se pueden agregar riesgos a una póliza cancelada");
        if (tipo == TipoPoliza.INDIVIDUAL && !riesgos.isEmpty()) {
            throw new ReglaNegocioException("Una póliza individual solo puede tener 1 riesgo");
        }
        Riesgo riesgo = new Riesgo(this, direccionInmueble, arrendatario, arrendador, canonMensual, vigenciaMeses);
        riesgos.add(riesgo);
        recalcularTotales();
        return riesgo;
    }

    /**
     * Renueva la póliza por el mismo periodo de vigencia inicial, incrementando el canon de cada
     * riesgo activo según el IPC y recalculando la prima.
     * Regla: no se puede renovar una póliza cancelada.
     *
     * @param ipcPorcentaje IPC en porcentaje (ej. 5.20 = 5,20 %)
     */
    public void renovar(BigDecimal ipcPorcentaje) {
        validarNoCancelada("No se puede renovar una póliza cancelada");
        if (ipcPorcentaje == null || ipcPorcentaje.signum() < 0) {
            throw new ReglaNegocioException("El IPC debe ser un valor mayor o igual a 0");
        }
        BigDecimal factor = BigDecimal.ONE.add(ipcPorcentaje.divide(CIEN, 6, RoundingMode.HALF_UP));
        riesgosActivos().forEach(riesgo -> riesgo.incrementarCanon(factor, vigenciaMeses));

        this.fechaInicio = this.fechaFin;
        this.fechaFin = this.fechaInicio.plusMonths(vigenciaMeses);
        this.numeroRenovaciones++;
        this.estado = EstadoPoliza.RENOVADA;
        recalcularTotales();
    }

    /**
     * Cancela la póliza.
     * Regla: la cancelación de una póliza cancela todos sus riesgos.
     */
    public void cancelar() {
        validarNoCancelada("La póliza ya se encuentra cancelada");
        riesgosActivos().forEach(Riesgo::cancelar);
        this.estado = EstadoPoliza.CANCELADA;
        this.fechaCancelacion = LocalDate.now();
    }

    /**
     * Cancela un riesgo puntual de la póliza y recalcula canon y prima.
     * En una póliza individual el riesgo no se cancela por separado: se cancela la póliza.
     */
    public void cancelarRiesgo(Riesgo riesgo) {
        if (!riesgos.contains(riesgo)) {
            throw new ReglaNegocioException("El riesgo no pertenece a la póliza " + numeroPoliza);
        }
        validarNoCancelada("La póliza está cancelada; sus riesgos ya fueron cancelados");
        if (tipo == TipoPoliza.INDIVIDUAL) {
            throw new ReglaNegocioException(
                    "El riesgo de una póliza individual no se cancela por separado; cancele la póliza");
        }
        riesgo.cancelar();
        recalcularTotales();
    }

    // ------------------------------------------------------------------ apoyo

    private void validarNoCancelada(String mensaje) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    private List<Riesgo> riesgosActivos() {
        return riesgos.stream().filter(Riesgo::estaActivo).toList();
    }

    /** prima = canon mensual total × meses de vigencia (solo riesgos activos). */
    private void recalcularTotales() {
        this.canonMensual = riesgosActivos().stream()
                .map(Riesgo::getCanonMensual)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        this.prima = canonMensual.multiply(BigDecimal.valueOf(vigenciaMeses)).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ getters

    public Long getId() {
        return id;
    }

    public String getNumeroPoliza() {
        return numeroPoliza;
    }

    public TipoPoliza getTipo() {
        return tipo;
    }

    public EstadoPoliza getEstado() {
        return estado;
    }

    public String getTomador() {
        return tomador;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public int getVigenciaMeses() {
        return vigenciaMeses;
    }

    public BigDecimal getCanonMensual() {
        return canonMensual;
    }

    public BigDecimal getPrima() {
        return prima;
    }

    public int getNumeroRenovaciones() {
        return numeroRenovaciones;
    }

    public LocalDate getFechaCancelacion() {
        return fechaCancelacion;
    }

    public List<Riesgo> getRiesgos() {
        return Collections.unmodifiableList(riesgos);
    }

    public long getCantidadRiesgosActivos() {
        return riesgos.stream().filter(Riesgo::estaActivo).count();
    }
}
