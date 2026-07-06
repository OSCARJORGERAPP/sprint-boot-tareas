# RETROSPECTIVA — tareas-api

Problemas encontrados durante el desarrollo (2026-07-05) y cómo se resolvieron.

## 1. No había Java ni Maven en la máquina

**Síntoma:** `java: The term 'java' is not recognized...` — el directorio del
proyecto estaba vacío y no existía ningún JDK instalado.

**Solución:** instalación a nivel de usuario (sin administrador) con Scoop:

```powershell
scoop bucket add java
scoop install java/temurin21-jdk maven
```

Quedaron Temurin JDK 21.0.11 y Maven 3.9.16. Detalle: la sesión de shell ya
abierta no ve el PATH actualizado; hay que abrir una nueva o anteponer las
rutas de `scoop\apps\...\current\bin` manualmente.

## 2. `TestRestTemplate` no compilaba: el paquete ya no existe en Spring Boot 4

**Síntoma:**

```
package org.springframework.boot.test.web.client does not exist
```

**Causa:** Spring Initializr generó el proyecto con **Spring Boot 4.1**, que
modularizó los starters (`spring-boot-starter-webmvc`,
`spring-boot-starter-webmvc-test`, etc.) y **movió `TestRestTemplate`** fuera
de su ubicación clásica de Boot 2/3 (`org.springframework.boot.test.web.client`).
Casi toda la documentación y ejemplos en internet siguen mostrando el paquete viejo.

**Solución:** inspeccionar los jars descargados en `~/.m2` para localizar la
clase. La ubicación real en Boot 4.1 es:

```java
import org.springframework.boot.resttestclient.TestRestTemplate;
```

(módulo `spring-boot-resttestclient`), y hubo que agregar al `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web-server-test</artifactId>
    <scope>test</scope>
</dependency>
```

Un intento intermedio con `org.springframework.boot.web.server.test.client`
también falló — ese paquete tampoco existe en 4.1.

## 3. El bean `TestRestTemplate` no se inyectaba

**Síntoma:** los 11 tests fallaban al arrancar con:

```
NoSuchBeanDefinitionException: No qualifying bean of type
'org.springframework.boot.resttestclient.TestRestTemplate' available
```

**Causa:** en Boot 2/3, `@SpringBootTest(webEnvironment = RANDOM_PORT)`
registraba el bean automáticamente. En Boot 4 ya no: hay que pedirlo
explícitamente.

**Solución:** anotar la clase de test con:

```java
@AutoConfigureTestRestTemplate   // org.springframework.boot.resttestclient.autoconfigure
```

## 4. `NoClassDefFoundError: RestTemplateBuilder`

**Síntoma:** con la anotación puesta, los tests seguían cayendo con:

```
java.lang.NoClassDefFoundError: org/springframework/boot/restclient/RestTemplateBuilder
```

**Causa:** otra consecuencia de la modularización de Boot 4:
`TestRestTemplate` depende en runtime de `RestTemplateBuilder`, que vive en el
módulo `spring-boot-restclient`, y ningún starter del proyecto lo arrastraba.

**Solución:** agregar la dependencia (scope `test`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-restclient</artifactId>
    <scope>test</scope>
</dependency>
```

Con esto: `Tests run: 12, Failures: 0, Errors: 0 — BUILD SUCCESS`.

## 5. Menor: anotación `@ResponseStatus` incompleta en el controller

**Síntoma:** quedó un `@ResponseStatus` sin valor ni import sobre el método
`eliminar` del controller (residuo de un cambio de enfoque a mitad de
escritura).

**Solución:** eliminar la anotación; el método ya devuelve
`ResponseEntity.status(HttpStatus.NO_CONTENT)` explícitamente.

## Lecciones aprendidas

- **Spring Boot 4 rompió las rutas de paquetes de test más citadas.** Ante un
  "package does not exist" con Boot 4, no confiar en ejemplos de Boot 2/3:
  buscar la clase dentro de los jars de `~/.m2` es la vía más rápida a la
  verdad (`spring-boot-resttestclient` + `@AutoConfigureTestRestTemplate` +
  `spring-boot-restclient`).
- **Fijarse en la versión que genera Initializr** (aquí 4.1.0): si se quiere
  compatibilidad con tutoriales existentes, puede convenir fijar Boot 3.x
  explícitamente al generar.
- Los tres fallos de test (2, 3 y 4) fueron capas de la misma cebolla:
  compilación → bean faltante → clase de runtime faltante. Resolverlos en
  orden, releyendo el error real de cada capa, evitó dar vueltas.
