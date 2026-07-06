# tareas-api

API REST para un CRUD de lista de tareas, con interfaz web incluida.

- **Arquitectura:** Spring Boot 4.1 (Java 21), capas controller → service → repository (Spring Data JPA).
- **Base de datos:** SQL vía H2 embebida (`jdbc:h2:mem:tareasdb`). Para usar MySQL/PostgreSQL basta cambiar el datasource en `application.properties` y agregar el driver al `pom.xml`.
- **Interfaz de usuario:** página web en HTML + JavaScript puro servida por la propia app en `http://localhost:8080/` (`src/main/resources/static/index.html`). Permite crear, completar, eliminar y filtrar tareas contra la API.
- **Historial de cambios:** ver `CHANGELOG.md`. Los problemas encontrados y sus soluciones están en `RETROSPECTIVA.md`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/tareas` | Lista todas las tareas (`?completada=true/false` filtra por estado) |
| GET | `/api/tareas/{id}` | Obtiene una tarea (404 si no existe) |
| POST | `/api/tareas` | Crea una tarea (201 + header `Location`; 400 si falla validación) |
| PUT | `/api/tareas/{id}` | Actualiza título, descripción y estado |
| DELETE | `/api/tareas/{id}` | Elimina la tarea (204) |

Modelo de tarea:

```json
{ "titulo": "Comprar pan", "descripcion": "opcional", "completada": false }
```

`titulo` es obligatorio (máx. 100 caracteres); `descripcion` máx. 500.

## Ejecutar

```
mvn spring-boot:run
```

La consola H2 queda en `http://localhost:8080/h2-console`.

## Tests de integración

```
mvn test
```

`TareaIntegrationTest` levanta el contexto completo (`@SpringBootTest` con puerto aleatorio) y ejercita la API por HTTP real con `TestRestTemplate` contra la base H2: creación, validación (400), lectura, filtro, 404, actualización, borrado y el ciclo de vida completo.
