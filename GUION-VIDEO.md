# GUION-VIDEO — tareas-api (máx. 5 minutos)

Guion para el video de entrega. Tiempos orientativos por bloque; total ≈ 4:50,
con margen para respirar. **Antes de grabar:** tener la app corriendo
(`mvn spring-boot:run`), el navegador en `http://localhost:8080/` con la lista
vacía, y el editor abierto con los archivos que se van a mostrar.

---

## Bloque 1 — Presentación del proyecto (0:00 – 0:30)

**En pantalla:** `README.md` abierto.

**Decir:**
> "Este proyecto es una API REST para gestionar una lista de tareas,
> construida con **Spring Boot 4.1 y Java 21**, con **SQL como base de
> datos** —H2 embebida, intercambiable por MySQL o PostgreSQL— y una
> **interfaz web** incluida. El CRUD completo está cubierto por **tests de
> integración** que ejercitan la API por HTTP real."

Mencionar la arquitectura en una frase: controller → service → repository.

---

## Bloque 2 — Demo de la interfaz de usuario (0:30 – 2:00)

**En pantalla:** navegador en `http://localhost:8080/`.

Secuencia de acciones (ir narrando cada una):

1. **(0:30)** Mostrar la página: formulario oscuro arriba, filtros, lista vacía.
2. **(0:40) Crear** una tarea: título "Preparar la entrega", descripción
   "Grabar el video del proyecto" → clic en *Agregar tarea*. Aparece en la lista
   con su fecha de creación.
3. **(1:00) Validación:** intentar agregar una tarea con el título vacío →
   se ve el mensaje de error que devuelve el backend (HTTP 400).
   > "La validación no es solo del navegador: es Bean Validation en la
   > entidad, y el backend responde 400 con el detalle por campo."
4. **(1:15)** Crear una segunda tarea (ej.: "Revisar documentación").
5. **(1:25) Completar:** marcar el checkbox de la primera → el título se tacha.
6. **(1:35) Filtrar:** clic en *Completadas* → solo se ve la primera; clic en
   *Pendientes* → solo la segunda; volver a *Todas*.
7. **(1:45) Eliminar:** botón 🗑 de la segunda tarea → desaparece.
8. **(1:50) La base SQL de verdad:** abrir pestaña con
   `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:tareasdb`,
   usuario `sa`, sin password) y ejecutar `SELECT * FROM TAREAS;`.
   > "Aquí se ve la fila persistida en la tabla: la interfaz consume la API
   > y la API persiste en SQL."

---

## Bloque 3 — Explicación del código (2:00 – 4:00)

**En pantalla:** editor. Mostrar cada archivo unos 20–25 segundos, señalando
solo lo importante (no leer el código línea a línea).

1. **(2:00) `model/Tarea.java`** — la entidad JPA:
   > "La entidad `Tarea` mapea a la tabla `tareas`: id autogenerado, título
   > obligatorio de hasta 100 caracteres con Bean Validation, estado
   > `completada` y fecha de creación que se asigna sola con `@PrePersist`."

2. **(2:25) `repository/TareaRepository.java`** — acceso a datos:
   > "El repositorio extiende `JpaRepository`: el CRUD viene gratis, y
   > `findByCompletada` lo genera Spring Data a partir del nombre del método."

3. **(2:45) `service/TareaService.java`** — lógica de negocio:
   > "El servicio concentra la lógica y las transacciones. Si una tarea no
   > existe lanza `TareaNoEncontradaException`, que un handler global
   > convierte en 404."

4. **(3:05) `controller/TareaController.java`** — la API REST:
   > "El controller expone `/api/tareas`: GET con filtro opcional, POST que
   > devuelve 201 con header `Location`, PUT y DELETE. `@Valid` dispara la
   > validación en la entrada."

5. **(3:25) `exception/GlobalExceptionHandler.java`** — manejo de errores:
   > "Con `@RestControllerAdvice` centralizamos los errores: 404 con mensaje
   > claro y 400 con un mapa campo → error, que es justo lo que la interfaz
   > muestra al usuario."

6. **(3:40) `static/index.html`** — la interfaz (mostrar rápido):
   > "La interfaz es HTML y JavaScript puro servido por la propia app desde
   > `static/`: consume la API con `fetch`, sin frameworks ni build extra."

---

## Bloque 4 — Tests de integración (4:00 – 4:40)

**En pantalla:** `TareaIntegrationTest.java`, luego la terminal.

1. **(4:00)** Mostrar la cabecera de la clase:
   > "Los tests de integración levantan el contexto completo con
   > `@SpringBootTest` en un puerto aleatorio y llaman a la API por **HTTP
   > real** con `TestRestTemplate`, contra la base H2. No se mockea nada:
   > se prueba el sistema de punta a punta."
2. **(4:10)** Hacer scroll rápido por los casos: crear, validación 400,
   listar, filtrar, 404, actualizar, eliminar y el ciclo de vida completo.
3. **(4:20)** En la terminal, ejecutar `mvn test` (o mostrar una ejecución ya
   hecha si el tiempo aprieta) y enfocar el resultado:
   > "**Tests run: 12, Failures: 0, Errors: 0 — BUILD SUCCESS.**"

---

## Bloque 5 — Cierre (4:40 – 5:00)

**En pantalla:** volver al `README.md` o a la interfaz.

**Decir:**
> "En resumen: una API CRUD de tareas en Spring Boot con persistencia SQL,
> arquitectura en capas, validación y manejo de errores centralizado,
> interfaz web integrada y una suite de tests de integración que valida el
> flujo completo. Todo el recorrido del proyecto está documentado en el
> CHANGELOG, el QUICKSTART y la RETROSPECTIVA del repositorio. Gracias."

---

## Checklist previo a grabar

- [ ] App corriendo (`mvn spring-boot:run`) y `http://localhost:8080/` carga.
- [ ] Base vacía (reiniciar la app si hay datos de pruebas anteriores).
- [ ] Consola H2 probada: JDBC URL `jdbc:h2:mem:tareasdb`, usuario `sa`, sin password.
- [ ] Editor con los 6 archivos del Bloque 3 abiertos en pestañas, en orden.
- [ ] `mvn test` ejecutado recientemente (por si se muestra el resultado en vez de correrlo en vivo).
- [ ] Zoom/tamaño de fuente del editor legible en la resolución de grabación.
