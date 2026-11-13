# API de Gestión de Pólizas

API REST para la gestión de pólizas de arrendamiento **individuales** y **colectivas**: consulta, renovación por IPC, cancelación y administración de riesgos. Cada operación que modifica estados se propaga al CORE de seguros a través de un servicio de edición, simulado aquí con un mock.

## Stack

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Validation) |
| Base de datos | H2 en memoria, con datos de ejemplo cargados al iniciar |
| Documentación | OpenAPI / Swagger UI |
| Pruebas | JUnit 5, AssertJ, MockMvc, Mockito |
| Build | Maven (incluye Maven Wrapper) |

## Cómo ejecutar

**Requisito:** JDK 21. No hace falta instalar Maven ni una base de datos.

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

**Desde IntelliJ IDEA:** `File → Open →` carpeta del proyecto. IntelliJ lo importa como proyecto Maven. Luego se ejecuta la clase `PolizasApiApplication`.

La API queda disponible en `http://localhost:8080`.

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html (botón **Authorize** → `123456`) |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:polizasdb`, usuario `sa`, sin clave) |

### Ejecutar las pruebas

```bash
mvnw.cmd test        # Windows
./mvnw test          # Linux / macOS
```

Hay 25 pruebas: 9 unitarias de reglas de negocio (`PolizaTest`) y 16 de integración de los endpoints (`PolizasApiIntegrationTest`).

## Seguridad

Todas las peticiones requieren el header:

```
x-api-key: 123456
```

Sin él, o con un valor incorrecto, la API responde **401**. El valor se configura en `application.yml` (`security.api-key`) o con la variable de entorno `API_KEY`. Swagger y la consola H2 quedan excluidos para facilitar las pruebas locales.

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/polizas?tipo=&estado=` | Lista pólizas por tipo (`INDIVIDUAL`/`COLECTIVA`) y estado (`ACTIVA`/`RENOVADA`/`CANCELADA`). Ambos filtros son opcionales y no distinguen mayúsculas. | 200, 400 |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza | 200, 404 |
| POST | `/polizas/{id}/renovar` | Incrementa canon y prima en +IPC; el estado pasa a `RENOVADA` | 200, 404, 422 |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos | 200, 404, 422 |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo pólizas colectivas) | 201, 400, 404, 422 |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo y recalcula la prima de la póliza | 200, 404, 422 |
| POST | `/core-mock/evento` | Mock del CORE: registra en logs la operación enviada | 202, 400 |

Los errores siguen el estándar **RFC 7807** (`application/problem+json`):

```json
{
  "title": "Regla de negocio incumplida",
  "status": 422,
  "detail": "No se puede renovar una póliza cancelada",
  "instance": "/polizas/3/renovar"
}
```

### Ejemplos con curl

```bash
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"

curl -H "x-api-key: 123456" http://localhost:8080/polizas/2/riesgos

curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
     -d '{"direccionInmueble":"Calle 80 # 10-20","arrendatario":"Ana Martínez","arrendador":"Luis Romero","canonMensual":1200000}' \
     http://localhost:8080/polizas/2/riesgos

curl -X POST -H "x-api-key: 123456" http://localhost:8080/riesgos/4/cancelar

curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/2/cancelar
```

El archivo [`http/polizas.http`](http/polizas.http) tiene todas las peticiones listas para ejecutar desde el cliente HTTP de IntelliJ.

## Datos de ejemplo

| id | Número | Tipo | Estado | Riesgos (id) | Canon total | Prima |
|---|---|---|---|---|---|---|
| 1 | IND-0001 | INDIVIDUAL | ACTIVA | 1 | 1.000.000 | 12.000.000 (12 meses) |
| 2 | COL-0001 | COLECTIVA | ACTIVA | 2, 3, 4 | 4.600.000 | 55.200.000 (12 meses) |
| 3 | COL-0002 | COLECTIVA | CANCELADA | 5, 6 (cancelados) | — | — |
| 4 | IND-0002 | INDIVIDUAL | ACTIVA | 7 | 2.500.000 | 15.000.000 (6 meses) |

## Reglas de negocio

1. **Una póliza individual solo puede tener 1 riesgo.**
2. **No se puede renovar una póliza cancelada.**
3. **La cancelación de una póliza cancela todos sus riesgos.**
4. **Agregar un riesgo exige validar el tipo de póliza:** solo se permite en pólizas `COLECTIVA` que no estén canceladas.
5. **Prima = canon mensual × meses de vigencia.** En una colectiva, el canon y la prima de la póliza son la suma de sus riesgos activos.
6. **Renovación:** se mantiene la misma vigencia inicial, el canon de cada riesgo activo se incrementa según el IPC (configurable en `polizas.ipc`, por defecto 5,20 %) y se recalcula la prima.
7. **Cancelar un riesgo recalcula** el canon y la prima de la póliza. El riesgo de una póliza individual no se cancela por separado: se cancela la póliza.
8. Toda acción que modifica estados de pólizas o riesgos **se envía al CORE** (`{"evento":"ACTUALIZACION","polizaId":...}`).

## Arquitectura

Arquitectura en capas, con el dominio en el centro:

```
src/main/java/com/prueba/polizas
├── controller/    Endpoints REST (PolizaController, RiesgoController, CoreMockController)
├── service/       Casos de uso y transacciones (PolizaService, RiesgoService)
├── repository/    Acceso a datos con Spring Data JPA
├── domain/        Entidades Poliza y Riesgo con las reglas de negocio + enums de tipo/estado
├── dto/           Contratos de entrada y salida (records)
├── integration/   Puerto CoreClient, adaptador REST y listener de sincronización con el CORE
├── exception/     Excepciones de negocio y manejador global (RFC 7807)
└── config/        Filtro x-api-key, propiedades, OpenAPI y carga de datos de ejemplo
```

### Decisiones de diseño

- **Modelo de dominio rico.** Las reglas viven en `Poliza` (agregado raíz) y no en los servicios. Así ninguna capa puede dejar una póliza en estado inválido, y las reglas se prueban con tests unitarios sin levantar Spring.
- **El canon pertenece al riesgo.** En una póliza colectiva cada inmueble tiene su propio arriendo; la póliza expone totales calculados. Una individual es una póliza con un único riesgo, así que la lógica es la misma para ambos tipos.
- **`BigDecimal` para valores monetarios**, con escala 2 y redondeo `HALF_UP`, para evitar errores de precisión de `double`.
- **Integración con el CORE desacoplada.** Los servicios publican un evento de dominio (`PolizaModificadaEvent`) y un listener lo envía al CORE **después del commit** (`@TransactionalEventListener(AFTER_COMMIT)`), de modo que nunca se informa un cambio que terminó revirtiéndose. El CORE se accede a través de la interfaz `CoreClient` (puerto); la implementación HTTP (`RestCoreClient`) es reemplazable sin tocar el negocio.
- **Resiliencia básica.** El cliente del CORE tiene timeouts configurables. Si el CORE falla, la operación de negocio no se revierte y el error queda registrado en logs.
- **Bloqueo optimista** (`@Version`) en la póliza para evitar actualizaciones concurrentes; un conflicto responde 409.
- **Consultas eficientes.** `@EntityGraph` evita el problema N+1 al listar pólizas con sus riesgos, hay un índice por `(tipo, estado)` y `open-in-view` está deshabilitado.
- **Endpoints de acción** (`/renovar`, `/cancelar`): son transiciones de estado con reglas de negocio, no actualizaciones de campos.

### Qué se agregaría en un entorno productivo

- Tabla **outbox** + broker de mensajería (Kafka/RabbitMQ) con reintentos y Dead Letter Queue para la sincronización con el CORE, más Circuit Breaker (Resilience4j).
- Base de datos persistente (Oracle/PostgreSQL) con migraciones versionadas (Flyway/Liquibase).
- Autenticación OAuth2/JWT en un API Gateway, en lugar de una API key fija.
- Versionamiento de rutas (`/api/v1/...`), paginación en los listados, logs estructurados con `correlationId` y métricas con Micrometer/Prometheus.
- Creación y modificación de pólizas, y renovación automática programada (`@Scheduled` + ShedLock).
