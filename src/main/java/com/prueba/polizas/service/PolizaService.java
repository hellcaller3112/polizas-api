package com.prueba.polizas.service;

import com.prueba.polizas.config.PolizasProperties;
import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.Riesgo;
import com.prueba.polizas.domain.TipoPoliza;
import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.dto.RiesgoRequest;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.exception.RecursoNoEncontradoException;
import com.prueba.polizas.exception.ReglaNegocioException;
import com.prueba.polizas.integration.PolizaModificadaEvent;
import com.prueba.polizas.repository.PolizaRepository;
import com.prueba.polizas.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PolizaService {

    private static final Logger log = LoggerFactory.getLogger(PolizaService.class);

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final ApplicationEventPublisher eventos;
    private final PolizasProperties propiedades;

    public PolizaService(PolizaRepository polizaRepository, RiesgoRepository riesgoRepository,
                         ApplicationEventPublisher eventos, PolizasProperties propiedades) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.eventos = eventos;
        this.propiedades = propiedades;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream()
                .map(PolizaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        return obtener(polizaId).getRiesgos().stream()
                .map(RiesgoResponse::from)
                .toList();
    }

    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = obtener(polizaId);
        poliza.renovar(propiedades.ipc());
        log.info("Póliza {} renovada con IPC {}%. Canon: {} - Prima: {}",
                poliza.getNumeroPoliza(), propiedades.ipc(), poliza.getCanonMensual(), poliza.getPrima());
        notificarCambio(poliza, "RENOVACION");
        return PolizaResponse.from(poliza);
    }

    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = obtener(polizaId);
        poliza.cancelar();
        log.info("Póliza {} cancelada junto con todos sus riesgos", poliza.getNumeroPoliza());
        notificarCambio(poliza, "CANCELACION_POLIZA");
        return PolizaResponse.from(poliza);
    }

    public RiesgoResponse agregarRiesgo(Long polizaId, RiesgoRequest solicitud) {
        Poliza poliza = obtener(polizaId);
        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException("Solo se pueden agregar riesgos a pólizas colectivas");
        }
        Riesgo riesgo = poliza.agregarRiesgo(
                solicitud.direccionInmueble(),
                solicitud.arrendatario(),
                solicitud.arrendador(),
                solicitud.canonMensual());
        riesgoRepository.save(riesgo);
        log.info("Riesgo {} agregado a la póliza {}. Nueva prima: {}",
                riesgo.getId(), poliza.getNumeroPoliza(), poliza.getPrima());
        notificarCambio(poliza, "INCLUSION_RIESGO");
        return RiesgoResponse.from(riesgo);
    }

    Poliza obtener(Long polizaId) {
        return polizaRepository.findById(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza", polizaId));
    }

    /**
     * Toda acción que modifica estados se propaga al CORE. El evento se procesa después del commit
     * (ver CoreSyncListener) para no notificar cambios que finalmente se revirtieron.
     */
    private void notificarCambio(Poliza poliza, String operacion) {
        eventos.publishEvent(new PolizaModificadaEvent(poliza.getId(), operacion));
    }
}
