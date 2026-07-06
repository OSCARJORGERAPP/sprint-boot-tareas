# QUICKSTART — tareas-api

Guía paso a paso para levantar la app y verla funcionando.

## 1. Requisitos

- **Java 21** (JDK) y **Maven 3.9+**.

Si no los tienes, la vía más simple en Windows es [Scoop](https://scoop.sh):

```powershell
scoop bucket add java
scoop install java/temurin21-jdk maven
```

Verifica que quedaron disponibles:

```powershell
java -version   # debe decir 21.x
mvn -version    # debe decir 3.9.x
```

> Si el terminal no reconoce `java`/`mvn` justo después de instalar, abre una
> ventana nueva de PowerShell (Scoop actualiza el PATH de usuario) o antepón
> las rutas manualmente:
>
> ```powershell
> $env:Path = "$env:USERPROFILE\scoop\apps\temurin21-jdk\current\bin;$env:USERPROFILE\scoop\apps\maven\current\bin;$env:Path"
> ```

## 2. Arrancar la aplicación

Desde la carpeta del proyecto:

```powershell
mvn spring-boot:run
```

La primera vez Maven descarga dependencias (puede tardar unos minutos).
La app queda lista cuando ves en el log algo como:

```
Tomcat started on port 8080 (http)
Started TareasApiApplication in X.XXX seconds
```

Para detenerla: `Ctrl+C`.

> **Importante:** la base de datos H2 es **en memoria** — los datos se
> pierden cada vez que reinicias la app.

## 3. Ver la app funcionando

### Opción A: interfaz web (la forma más fácil)

Abre <http://localhost:8080/> — la app sirve una interfaz de usuario donde puedes:

- **Crear tareas** con el formulario (título obligatorio, descripción opcional);
  si el título está vacío o excede 100 caracteres verás el mensaje de validación del backend.
- **Marcar completada/pendiente** con el checkbox de cada tarea.
- **Eliminar** con el botón 🗑.
- **Filtrar** con los botones Todas / Pendientes / Completadas.

### Opción B: navegador (API y base de datos directamente)

- <http://localhost:8080/api/tareas> — lista de tareas en JSON (vacía `[]` al inicio).
- <http://localhost:8080/h2-console> — consola de la base SQL (ver guía abajo).

#### Consola H2 paso a paso (opcional)

La consola H2 es un visor web de la base de datos: sirve para ver con tus ojos
las filas SQL que la API guarda. La app funciona igual sin usarla.

1. Con la app corriendo, abre `http://localhost:8080/h2-console`
   (ojo con el autocompletado del navegador: la URL termina en `console`, con "e").
2. En la pantalla de login, los valores por defecto **no** son los de esta app.
   Configura:

   | Campo | Valor |
   |---|---|
   | Driver Class | `org.h2.Driver` (ya viene así) |
   | **JDBC URL** | `jdbc:h2:mem:tareasdb` ← el que hay que cambiar |
   | User Name | `sa` |
   | Password | (vacío) |

3. Clic en **Connect**.
4. En el recuadro de consulta escribe y ejecuta (**Run** o `Ctrl+Enter`):

   ```sql
   SELECT * FROM TAREAS;
   ```

   Abajo aparece la tabla con id, título, descripción, completada y fecha.

#### Ver en la consola una tarea cargada desde el formulario

Trabaja con dos pestañas del navegador:

1. **Pestaña 1 — interfaz** (<http://localhost:8080/>): carga la tarea con el
   formulario y verifica que aparece en la lista.
2. **Pestaña 2 — consola H2** (ya conectada, paso anterior): ejecuta
   `SELECT * FROM TAREAS;` con **Run** (o `Ctrl+Enter`). Abajo aparece la fila
   con `ID`, `TITULO`, `DESCRIPCION`, `COMPLETADA` y `FECHA_CREACION`.

El detalle clave: **la consola no se actualiza sola.** Cada vez que hagas un
cambio en la interfaz, vuelve a la pestaña de la consola y pulsa **Run** de
nuevo para re-ejecutar la consulta:

- Cargas otra tarea → aparece una fila nueva.
- Marcas una como completada → la columna `COMPLETADA` pasa a `TRUE`.
- La eliminas → la fila desaparece.

Esa ida y vuelta interfaz ↔ consola demuestra que la app persiste en una base
SQL real (es la demo sugerida en el minuto 1:50 de `GUION-VIDEO.md`).

Errores típicos:

- *"Database not found"* → el campo JDBC URL no dice exactamente `jdbc:h2:mem:tareasdb`.
- *Whitelabel Error Page (404)* → la URL está mal escrita (p. ej. `/h2-consol` sin la "e").
- Recuerda: la consola solo encuentra la base **mientras la app está corriendo**,
  porque H2 vive en la memoria del proceso.

### Opción C: PowerShell (CRUD completo)

```powershell
# CREAR (POST) → devuelve 201 con la tarea y su id
Invoke-RestMethod -Uri "http://localhost:8080/api/tareas" -Method Post `
  -ContentType "application/json" `
  -Body '{"titulo":"Comprar pan","descripcion":"Antes de las 10"}'

# LISTAR (GET)
Invoke-RestMethod -Uri "http://localhost:8080/api/tareas"

# LISTAR SOLO COMPLETADAS / PENDIENTES
Invoke-RestMethod -Uri "http://localhost:8080/api/tareas?completada=true"

# OBTENER UNA (GET por id — 404 si no existe)
Invoke-RestMethod -Uri "http://localhost:8080/api/tareas/1"

# ACTUALIZAR (PUT) — marca la tarea 1 como completada
Invoke-RestMethod -Uri "http://localhost:8080/api/tareas/1" -Method Put `
  -ContentType "application/json" `
  -Body '{"titulo":"Comprar pan","descripcion":"Antes de las 10","completada":true}'

# ELIMINAR (DELETE) → 204 sin cuerpo
Invoke-WebRequest -Uri "http://localhost:8080/api/tareas/1" -Method Delete
```

### Opción D: curl / Postman

Los mismos endpoints funcionan con cualquier cliente REST:

```bash
curl -X POST http://localhost:8080/api/tareas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Mi tarea"}'
curl http://localhost:8080/api/tareas
```

## 4. Validaciones y errores que puedes provocar

- **400 Bad Request** — crea una tarea sin título (`{"titulo":""}`):
  la respuesta incluye un mapa `errores` con el detalle por campo.
- **404 Not Found** — pide un id inexistente (`GET /api/tareas/999`):
  la respuesta incluye `mensaje: "No se encontró la tarea con id 999"`.

## 5. Ejecutar los tests de integración

```powershell
mvn test
```

Ejecuta `TareaIntegrationTest`: 11 casos que levantan el contexto completo
de Spring en un puerto aleatorio y ejercitan la API por HTTP real contra la
base H2 (crear, validar, listar, filtrar, leer, actualizar, borrar, los 404
y un ciclo de vida completo). Resultado esperado: `BUILD SUCCESS`,
`Tests run: 12, Failures: 0, Errors: 0`.
