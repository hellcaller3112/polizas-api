package com.prueba.polizas.controller;

import com.prueba.polizas.dto.CoreEventoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mock del servicio agnóstico de edición (capa media WebLogic) que mantiene actualizado el CORE.
 * Su único propósito es registrar en logs que la operación se intentó enviar al CORE.
 */
@Tag(name = "CORE (mock)")
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @Operation(summary = "Registrar en logs un evento enviado al CORE")
    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> registrarEvento(@Valid @RequestBody CoreEventoRequest evento) {
        log.info("[CORE-MOCK] Operación enviada al CORE -> evento={}, polizaId={}", evento.evento(), evento.polizaId());
        return Map.of("estado", "REGISTRADO", "evento", evento.evento(), "polizaId", evento.polizaId());
    }
}
