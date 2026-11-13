package com.prueba.polizas.controller;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.TipoPoliza;
import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.dto.RiesgoRequest;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.service.PolizaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pólizas")
@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @Operation(summary = "Listar pólizas filtrando por tipo y/o estado")
    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado);
    }

    @Operation(summary = "Listar los riesgos de una póliza")
    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id);
    }

    @Operation(summary = "Renovar póliza: incrementa canon y prima según IPC y pasa a RENOVADA")
    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return polizaService.renovar(id);
    }

    @Operation(summary = "Cancelar póliza y todos sus riesgos")
    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelar(id);
    }

    @Operation(summary = "Agregar un riesgo (solo pólizas colectivas)")
    @PostMapping("/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    public RiesgoResponse agregarRiesgo(@PathVariable Long id, @Valid @RequestBody RiesgoRequest solicitud) {
        return polizaService.agregarRiesgo(id, solicitud);
    }
}
