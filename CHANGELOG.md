# CHANGELOG — tareas-api

Registro cronológico de cambios del proyecto. Cada cambio nuevo se documenta
aquí y, cuando corresponde, en `README.md`, `QUICKSTART.md` y `RETROSPECTIVA.md`.

## 2026-07-06

### Publicación en GitLab y GitHub + integración continua
- Repositorio git inicializado y publicado en:
  - GitLab: https://gitlab.codecrypto.academy/ojrapp/sprint-boot-tareas
  - GitHub: https://github.com/OSCARJORGERAPP/sprint-boot-tareas
- `.gitlab-ci.yml` (versión final): usa los templates compartidos
  `internos/templates-cicd` con el job `build` (runner tag `cloudrun`,
  buildah), igual que lottery/ecommerce; `wake_cloudrun_runners` y `deploy`
  desactivados. La primera versión (imagen Maven sin tags) quedaba en
  `pending` eterno — ver `RETROSPECTIVA.md` §6. Además, el job `build` se
  sobrescribe para no exportar la imagen como artifact: la imagen Java
  supera el límite de artifacts del GitLab (error 413) y el deploy está
  desactivado — ver `RETROSPECTIVA.md` §7.
- `Dockerfile` multi-stage: el stage de build ejecuta `mvn test package`
  (los tests en rojo rompen el pipeline); el stage final es JRE 21 + jar,
  escuchando en el puerto 3000 que espera la plataforma.
- `.github/workflows/ci.yml`: workflow de GitHub Actions que ejecuta
  `mvn test` directamente (GitHub sí tiene runners con contenedores).
- Verificado localmente antes del push: `Tests run: 12, Failures: 0, Errors: 0`.

### Cómo ver en la consola H2 una tarea cargada desde el formulario
- Se agregó a `QUICKSTART.md` la subsección "Ver en la consola una tarea
  cargada desde el formulario": flujo con dos pestañas (interfaz + consola),
  re-ejecutar el `SELECT` con Run tras cada cambio (la consola no se
  actualiza sola), y qué se ve en cada operación (fila nueva, `COMPLETADA`
  a `TRUE`, fila eliminada).

### Guía paso a paso de la consola H2 en QUICKSTART
- Se amplió `QUICKSTART.md` con una subsección "Consola H2 paso a paso":
  qué es y para qué sirve, los valores exactos del login (JDBC URL
  `jdbc:h2:mem:tareasdb`), el `SELECT` de prueba, una prueba en vivo con la
  interfaz, y los errores típicos ("Database not found" por URL JDBC
  incorrecta y 404 por escribir `/h2-consol` sin la "e" final).

### Guion para el video de entrega
- Se creó `GUION-VIDEO.md`: guion de ≈5 minutos en 5 bloques con tiempos —
  presentación, demo de la interfaz (CRUD + validación + consola H2),
  explicación del código archivo por archivo, tests de integración y cierre.
  Incluye checklist previo a la grabación.

### Formulario con fondo oscuro
- El formulario de creación de tareas en `index.html` ahora usa tema oscuro:
  tarjeta gris oscuro (`#1f2937`), campos de entrada casi negros (`#111827`)
  con texto claro y placeholders grises, y el mensaje de error de validación
  en un rojo más claro (`#f87171`) para mantener contraste sobre el fondo.
- El resto de la página (lista de tareas, filtros) conserva el tema claro.
- Requirió reiniciar la app (los archivos de `static/` se cargan al arrancar),
  lo que vació la base H2 en memoria.

### Interfaz de usuario web
- Se agregó `src/main/resources/static/index.html`: interfaz en HTML +
  JavaScript puro (sin frameworks) servida por la propia app en
  `http://localhost:8080/`.
- Funcionalidad: crear tareas (con mensajes de validación del backend),
  marcar completada/pendiente con checkbox, eliminar, y filtrar por
  Todas / Pendientes / Completadas usando `?completada=` de la API.
- Nota operativa: los archivos de `static/` se empaquetan al arrancar, por lo
  que hubo que reiniciar la app para que sirviera la página (al reiniciar se
  vació la base H2 en memoria).

### Documentación
- Se creó este `CHANGELOG.md` y se adoptó la regla de documentar todo cambio
  en los archivos `*.md` del proyecto.
- `README.md` y `QUICKSTART.md` actualizados con la interfaz web.

## 2026-07-05

### Creación del proyecto
- Proyecto generado con Spring Initializr: Spring Boot 4.1.0, Java 21, Maven.
- Dependencias: webmvc, Spring Data JPA, validación, H2 (base SQL embebida).
- Entorno instalado con Scoop: Temurin JDK 21.0.11 y Maven 3.9.16.

### API CRUD de tareas
- Entidad `Tarea` (id, titulo, descripcion, completada, fechaCreacion) con
  validación (`titulo` obligatorio, máx. 100; `descripcion` máx. 500).
- Capas: `TareaRepository` (JPA) → `TareaService` → `TareaController`.
- Endpoints en `/api/tareas`: GET (lista, con filtro `?completada=`),
  GET `/{id}`, POST (201 + `Location`), PUT `/{id}`, DELETE `/{id}` (204).
- `GlobalExceptionHandler`: 404 con mensaje y 400 con detalle por campo.

### Tests de integración
- `TareaIntegrationTest`: 11 casos con `@SpringBootTest` (puerto aleatorio)
  que ejercitan la API por HTTP real con `TestRestTemplate` contra H2.
- Ajustes por Spring Boot 4.1 (ver `RETROSPECTIVA.md`): nuevo paquete de
  `TestRestTemplate`, anotación `@AutoConfigureTestRestTemplate` y
  dependencias `spring-boot-starter-web-server-test` y
  `spring-boot-restclient`.
- Resultado: `Tests run: 12, Failures: 0, Errors: 0`.

### Documentación inicial
- `README.md` (arquitectura y endpoints), `QUICKSTART.md` (puesta en marcha)
  y `RETROSPECTIVA.md` (problemas y soluciones).
