package com.prueba.polizas.integration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Propaga al CORE cada cambio de estado de pólizas o riesgos.
 * Se ejecuta solo después de que la transacción se confirma (AFTER_COMMIT), así nunca se informa
 * al CORE un cambio que terminó revirtiéndose.
 */
@Component
public class CoreSyncListener {

    private final CoreClient coreClient;

    public CoreSyncListener(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alModificarPoliza(PolizaModificadaEvent evento) {
        coreClient.enviarActualizacion(evento.polizaId(), evento.operacion());
    }
}
