package com.prueba.polizas;

import com.prueba.polizas.integration.CoreClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de los endpoints con la base H2 y los datos de ejemplo:
 * 1 = IND-0001 (individual activa), 2 = COL-0001 (colectiva activa, riesgos 2, 3 y 4),
 * 3 = COL-0002 (colectiva cancelada), 4 = IND-0002 (individual activa).
 * <p>
 * El cliente del CORE se reemplaza por un mock para verificar que se invoca en cada cambio de estado.
 */
@SpringBootTest(properties = "polizas.ipc=5")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PolizasApiIntegrationTest {

    private static final String API_KEY = "123456";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CoreClient coreClient;

    private MockHttpServletRequestBuilder conApiKey(MockHttpServletRequestBuilder peticion) {
        return peticion.header("x-api-key", API_KEY);
    }

    @Test
    @DisplayName("Sin x-api-key responde 401")
    void sinApiKey() throws Exception {
        mvc.perform(get("/polizas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con x-api-key incorrecta responde 401")
    void apiKeyIncorrecta() throws Exception {
        mvc.perform(get("/polizas").header("x-api-key", "000000")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /polizas filtra por tipo y estado")
    void listarPorTipoYEstado() throws Exception {
        mvc.perform(conApiKey(get("/polizas").param("tipo", "Colectiva").param("estado", "ACTIVA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroPoliza").value("COL-0001"))
                .andExpect(jsonPath("$[0].riesgosActivos").value(3))
                .andExpect(jsonPath("$[0].prima").value(55200000.0));
    }

    @Test
    @DisplayName("GET /polizas sin filtros lista todas")
    void listarTodas() throws Exception {
        mvc.perform(conApiKey(get("/polizas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("GET /polizas con un tipo inexistente responde 400")
    void listarTipoInvalido() throws Exception {
        mvc.perform(conApiKey(get("/polizas").param("tipo", "OTRO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /polizas/{id}/riesgos lista los riesgos de la póliza")
    void listarRiesgos() throws Exception {
        mvc.perform(conApiKey(get("/polizas/2/riesgos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].polizaId").value(2));
    }

    @Test
    @DisplayName("GET /polizas/{id}/riesgos de una póliza inexistente responde 404")
    void polizaInexistente() throws Exception {
        mvc.perform(conApiKey(get("/polizas/999/riesgos")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /polizas/{id}/renovar incrementa canon y prima en +IPC y pasa a RENOVADA")
    void renovar() throws Exception {
        mvc.perform(conApiKey(post("/polizas/1/renovar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"))
                .andExpect(jsonPath("$.canonMensual").value(1050000.0))
                .andExpect(jsonPath("$.prima").value(12600000.0))
                .andExpect(jsonPath("$.numeroRenovaciones").value(1));

        verify(coreClient).enviarActualizacion(1L, "RENOVACION");
    }

    @Test
    @DisplayName("POST /polizas/{id}/renovar sobre una póliza cancelada responde 422 y no notifica al CORE")
    void noRenovarCancelada() throws Exception {
        mvc.perform(conApiKey(post("/polizas/3/renovar")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("No se puede renovar una póliza cancelada"));

        verify(coreClient, never()).enviarActualizacion(anyLong(), anyString());
    }

    @Test
    @DisplayName("POST /polizas/{id}/cancelar cancela la póliza y todos sus riesgos")
    void cancelarPoliza() throws Exception {
        mvc.perform(conApiKey(post("/polizas/2/cancelar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mvc.perform(conApiKey(get("/polizas/2/riesgos")))
                .andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADO"))));

        verify(coreClient).enviarActualizacion(2L, "CANCELACION_POLIZA");
    }

    @Test
    @DisplayName("POST /polizas/{id}/riesgos agrega un riesgo a una colectiva")
    void agregarRiesgoColectiva() throws Exception {
        mvc.perform(conApiKey(post("/polizas/2/riesgos"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble":"Calle 80 # 10-20","arrendatario":"Ana","arrendador":"Luis",
                                 "canonMensual":1200000}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.prima").value(14400000.0));

        mvc.perform(conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "ACTIVA")))
                .andExpect(jsonPath("$[0].riesgosActivos").value(4))
                .andExpect(jsonPath("$[0].prima").value(69600000.0));

        verify(coreClient).enviarActualizacion(2L, "INCLUSION_RIESGO");
    }

    @Test
    @DisplayName("POST /polizas/{id}/riesgos sobre una póliza individual responde 422")
    void agregarRiesgoIndividual() throws Exception {
        mvc.perform(conApiKey(post("/polizas/1/riesgos"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble":"Calle 80","arrendatario":"Ana","arrendador":"Luis",
                                 "canonMensual":1200000}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Solo se pueden agregar riesgos a pólizas colectivas"));
    }

    @Test
    @DisplayName("POST /polizas/{id}/riesgos con datos inválidos responde 400")
    void agregarRiesgoInvalido() throws Exception {
        mvc.perform(conApiKey(post("/polizas/2/riesgos"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble":"","arrendatario":"Ana","arrendador":"Luis","canonMensual":-5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.direccionInmueble").exists())
                .andExpect(jsonPath("$.errores.canonMensual").exists());
    }

    @Test
    @DisplayName("POST /riesgos/{id}/cancelar cancela el riesgo y recalcula la prima de la póliza")
    void cancelarRiesgo() throws Exception {
        mvc.perform(conApiKey(post("/riesgos/4/cancelar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        // 55.200.000 - (900.000 x 12) = 44.400.000
        mvc.perform(conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "ACTIVA")))
                .andExpect(jsonPath("$[0].prima").value(44400000.0))
                .andExpect(jsonPath("$[0].riesgosActivos").value(2));

        verify(coreClient).enviarActualizacion(2L, "EXCLUSION_RIESGO");
    }

    @Test
    @DisplayName("POST /riesgos/{id}/cancelar dos veces responde 422")
    void cancelarRiesgoDosVeces() throws Exception {
        mvc.perform(conApiKey(post("/riesgos/4/cancelar"))).andExpect(status().isOk());
        mvc.perform(conApiKey(post("/riesgos/4/cancelar")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El riesgo ya se encuentra cancelado"));
    }

    @Test
    @DisplayName("POST /core-mock/evento registra el evento")
    void coreMock() throws Exception {
        mvc.perform(conApiKey(post("/core-mock/evento"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evento":"ACTUALIZACION","polizaId":555}"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("REGISTRADO"));
    }
}
