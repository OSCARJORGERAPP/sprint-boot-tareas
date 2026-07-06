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

## 6. El pipeline de GitLab se quedaba en `pending` para siempre

**Síntoma:** tras el primer push a gitlab.codecrypto.academy, el pipeline llevaba
más de 9 minutos en `pending` sin que ningún runner tomara el job.

**Causa:** el `.gitlab-ci.yml` inicial usaba `image: maven:3.9-eclipse-temurin-21`
sin tags. En la infraestructura de la academia solo hay **un runner operativo,
con tag `cloudrun` y executor shell**: los jobs sin ese tag no los toma nadie
(pending eterno) y, además, el executor shell **ignora `image:`** — no existe
un contenedor Maven donde correr `mvn test`. Todo esto estaba ya documentado
en el AGENTS.md §CI de los proyectos lottery y ecommerce.

**Solución:** replicar el patrón probado de esos proyectos:

1. `.gitlab-ci.yml` incluye el template compartido
   `internos/templates-cicd → templates/build-deploy.yml`, cuyo job `build`
   (tag `cloudrun`) construye la imagen con buildah.
2. `wake_cloudrun_runners` y `deploy` desactivados con `rules: when: never`
   (con `allow_failure` el pipeline quedaría "passed with warnings", no verde limpio).
3. Los **tests corren dentro del build del Dockerfile**: el stage de Maven
   ejecuta `mvn test package`, de modo que un test en rojo rompe el build y el
   pipeline falla. Verde en `build` = tests en verde.

**Lección:** antes de escribir CI para una infraestructura compartida, leer la
documentación operativa de los proyectos que ya la sufrieron (AGENTS.md /
RETROSPECTIVA.md de lottery, ecommerce, bonos, videocapture).

## 7. El build pasó los tests pero el job falló al subir el artifact (413)

**Síntoma:** en el pipeline #1550 los tests corrieron en verde dentro del build
(`Tests run: 12, Failures: 0, Errors: 0`) y la imagen se construyó, pero el job
terminó en rojo al subir `image.tar.gz`:
`413 Request Entity Too Large` → `FATAL: too large`.

**Causa:** el job `build` del template exporta la imagen como artifact para que
`deploy` la use. Una imagen Java (JRE 21 + jar) comprimida supera el tamaño
máximo de artifacts configurado en este GitLab. En los proyectos Node
(lottery/ecommerce) la imagen standalone es mucho más pequeña y no chocaba con
el límite.

**Solución:** como `deploy` está desactivado, el artifact no lo consume nadie:
se sobrescribe el job `build` en `.gitlab-ci.yml` para que solo ejecute
`buildah build` (sin `push` a docker-archive ni `gzip`) y con `artifacts: null`.
El valor del job queda intacto: los tests se ejecutan dentro del Dockerfile y
el verde del build certifica los tests en verde.

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
