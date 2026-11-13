package com.prueba.polizas.service;

import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.Riesgo;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.exception.RecursoNoEncontradoException;
import com.prueba.polizas.integration.PolizaModificadaEvent;
import com.prueba.polizas.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RiesgoService {

    private static final Logger log = LoggerFactory.getLogger(RiesgoService.class);

    private final RiesgoRepository riesgoRepository;
    private final ApplicationEventPublisher eventos;

    public RiesgoService(RiesgoRepository riesgoRepository, ApplicationEventPublisher eventos) {
        this.riesgoRepository = riesgoRepository;
        this.eventos = eventos;
    }

    public RiesgoResponse cancelar(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Riesgo", riesgoId));
        Poliza poliza = riesgo.getPoliza();

        // La regla vive en el agregado: valida tipo/estado y recalcula canon y prima de la póliza.
        poliza.cancelarRiesgo(riesgo);

        log.info("Riesgo {} cancelado en la póliza {}. Nueva prima: {}",
                riesgoId, poliza.getNumeroPoliza(), poliza.getPrima());
        eventos.publishEvent(new PolizaModificadaEvent(poliza.getId(), "EXCLUSION_RIESGO"));
        return RiesgoResponse.from(riesgo);
    }
}
