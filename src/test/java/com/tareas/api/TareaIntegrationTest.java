package com.tareas.api;

import com.tareas.api.model.Tarea;
import com.tareas.api.repository.TareaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TareaIntegrationTest {

    private static final String BASE = "/api/tareas";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TareaRepository repository;

    @BeforeEach
    void limpiarBase() {
        repository.deleteAll();
    }

    private Tarea crearTareaEnBase(String titulo, String descripcion, boolean completada) {
        Tarea tarea = new Tarea(titulo, descripcion);
        tarea.setCompletada(completada);
        return repository.save(tarea);
    }

    // ---------- CREATE ----------

    @Test
    @DisplayName("POST /api/tareas crea una tarea y devuelve 201 con Location")
    void crearTarea() {
        Tarea nueva = new Tarea("Comprar pan", "Ir a la panadería antes de las 10");

        ResponseEntity<Tarea> respuesta = rest.postForEntity(BASE, nueva, Tarea.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation()).isNotNull();
        Tarea creada = respuesta.getBody();
        assertThat(creada).isNotNull();
        assertThat(creada.getId()).isNotNull();
        assertThat(creada.getTitulo()).isEqualTo("Comprar pan");
        assertThat(creada.isCompletada()).isFalse();
        assertThat(creada.getFechaCreacion()).isNotNull();

        // Verifica persistencia real en la base SQL
        assertThat(repository.findById(creada.getId())).isPresent();
    }

    @Test
    @DisplayName("POST /api/tareas sin título devuelve 400 con detalle de validación")
    void crearTareaSinTituloFalla() {
        Tarea invalida = new Tarea("", "sin título");

        ResponseEntity<Map<String, Object>> respuesta = rest.exchange(
                BASE, HttpMethod.POST, new HttpEntity<>(invalida),
                new ParameterizedTypeReference<>() {});

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).containsKey("errores");
        assertThat(repository.count()).isZero();
    }

    // ---------- READ ----------

    @Test
    @DisplayName("GET /api/tareas devuelve todas las tareas")
    void listarTareas() {
        crearTareaEnBase("Tarea 1", "desc 1", false);
        crearTareaEnBase("Tarea 2", "desc 2", true);

        ResponseEntity<List<Tarea>> respuesta = rest.exchange(
                BASE, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(2)
                .extracting(Tarea::getTitulo)
                .containsExactlyInAnyOrder("Tarea 1", "Tarea 2");
    }

    @Test
    @DisplayName("GET /api/tareas?completada=true filtra por estado")
    void listarTareasFiltradas() {
        crearTareaEnBase("Pendiente", null, false);
        crearTareaEnBase("Hecha", null, true);

        ResponseEntity<List<Tarea>> respuesta = rest.exchange(
                BASE + "?completada=true", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
        assertThat(respuesta.getBody().get(0).getTitulo()).isEqualTo("Hecha");
    }

    @Test
    @DisplayName("GET /api/tareas/{id} devuelve la tarea existente")
    void obtenerTareaPorId() {
        Tarea guardada = crearTareaEnBase("Estudiar", "Spring Boot", false);

        ResponseEntity<Tarea> respuesta = rest.getForEntity(BASE + "/" + guardada.getId(), Tarea.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getId()).isEqualTo(guardada.getId());
        assertThat(respuesta.getBody().getTitulo()).isEqualTo("Estudiar");
    }

    @Test
    @DisplayName("GET /api/tareas/{id} inexistente devuelve 404")
    void obtenerTareaInexistente() {
        ResponseEntity<Map<String, Object>> respuesta = rest.exchange(
                BASE + "/9999", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().get("mensaje").toString()).contains("9999");
    }

    // ---------- UPDATE ----------

    @Test
    @DisplayName("PUT /api/tareas/{id} actualiza título, descripción y estado")
    void actualizarTarea() {
        Tarea guardada = crearTareaEnBase("Original", "desc", false);

        Tarea cambios = new Tarea("Actualizada", "nueva desc");
        cambios.setCompletada(true);

        ResponseEntity<Tarea> respuesta = rest.exchange(
                BASE + "/" + guardada.getId(), HttpMethod.PUT,
                new HttpEntity<>(cambios), Tarea.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getTitulo()).isEqualTo("Actualizada");
        assertThat(respuesta.getBody().isCompletada()).isTrue();

        Tarea enBase = repository.findById(guardada.getId()).orElseThrow();
        assertThat(enBase.getTitulo()).isEqualTo("Actualizada");
        assertThat(enBase.isCompletada()).isTrue();
    }

    @Test
    @DisplayName("PUT /api/tareas/{id} inexistente devuelve 404")
    void actualizarTareaInexistente() {
        Tarea cambios = new Tarea("Cualquiera", null);

        ResponseEntity<Map<String, Object>> respuesta = rest.exchange(
                BASE + "/9999", HttpMethod.PUT, new HttpEntity<>(cambios),
                new ParameterizedTypeReference<>() {});

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- DELETE ----------

    @Test
    @DisplayName("DELETE /api/tareas/{id} elimina la tarea y devuelve 204")
    void eliminarTarea() {
        Tarea guardada = crearTareaEnBase("A borrar", null, false);

        ResponseEntity<Void> respuesta = rest.exchange(
                BASE + "/" + guardada.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(repository.existsById(guardada.getId())).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/tareas/{id} inexistente devuelve 404")
    void eliminarTareaInexistente() {
        ResponseEntity<Map<String, Object>> respuesta = rest.exchange(
                BASE + "/9999", HttpMethod.DELETE, null,
                new ParameterizedTypeReference<>() {});

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- CICLO COMPLETO ----------

    @Test
    @DisplayName("Ciclo de vida completo: crear → leer → actualizar → eliminar")
    void cicloDeVidaCompleto() {
        // Crear
        Tarea creada = rest.postForEntity(BASE, new Tarea("Ciclo", "completo"), Tarea.class).getBody();
        Long id = creada.getId();

        // Leer
        assertThat(rest.getForEntity(BASE + "/" + id, Tarea.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Actualizar
        creada.setCompletada(true);
        ResponseEntity<Tarea> actualizada = rest.exchange(
                BASE + "/" + id, HttpMethod.PUT, new HttpEntity<>(creada), Tarea.class);
        assertThat(actualizada.getBody().isCompletada()).isTrue();

        // Eliminar
        rest.exchange(BASE + "/" + id, HttpMethod.DELETE, null, Void.class);
        assertThat(rest.getForEntity(BASE + "/" + id, Tarea.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
