package com.tareas.api.service;

import com.tareas.api.exception.TareaNoEncontradaException;
import com.tareas.api.model.Tarea;
import com.tareas.api.repository.TareaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TareaService {

    private final TareaRepository repository;

    public TareaService(TareaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Tarea> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Tarea> listarPorEstado(boolean completada) {
        return repository.findByCompletada(completada);
    }

    @Transactional(readOnly = true)
    public Tarea buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TareaNoEncontradaException(id));
    }

    public Tarea crear(Tarea tarea) {
        tarea.setId(null);
        return repository.save(tarea);
    }

    public Tarea actualizar(Long id, Tarea datos) {
        Tarea existente = buscarPorId(id);
        existente.setTitulo(datos.getTitulo());
        existente.setDescripcion(datos.getDescripcion());
        existente.setCompletada(datos.isCompletada());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        Tarea existente = buscarPorId(id);
        repository.delete(existente);
    }
}
