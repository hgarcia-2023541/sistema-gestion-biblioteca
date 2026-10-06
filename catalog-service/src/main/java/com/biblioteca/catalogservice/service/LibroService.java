package com.biblioteca.catalogservice.service;

import com.biblioteca.catalogservice.dto.LibroRequest;
import com.biblioteca.catalogservice.dto.LibroResponse;
import com.biblioteca.catalogservice.entity.Libro;
import com.biblioteca.catalogservice.exception.BusinessRuleException;
import com.biblioteca.catalogservice.exception.ResourceNotFoundException;
import com.biblioteca.catalogservice.repository.LibroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LibroService {

    private final LibroRepository repository;

    public LibroService(LibroRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LibroResponse crear(LibroRequest req) {
        if (repository.existsByIsbn(req.getIsbn())) {
            throw new BusinessRuleException("Ya existe un libro con el ISBN: " + req.getIsbn());
        }
        validarStock(req.getStockTotal(), req.getStockDisponible());
        Libro l = new Libro();
        l.setIsbn(req.getIsbn());
        l.setTitulo(req.getTitulo());
        l.setAutor(req.getAutor());
        l.setCategoria(req.getCategoria());
        l.setStockTotal(req.getStockTotal());
        l.setStockDisponible(req.getStockDisponible());
        return LibroResponse.from(repository.save(l));
    }

    @Transactional(readOnly = true)
    public List<LibroResponse> listar() {
        return repository.findAll().stream().map(LibroResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return LibroResponse.from(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado: " + id)));
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest req) {
        Libro l = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado: " + id));
        validarStock(req.getStockTotal(), req.getStockDisponible());
        if (!l.getIsbn().equals(req.getIsbn()) && repository.existsByIsbn(req.getIsbn())) {
            throw new BusinessRuleException("Ya existe un libro con el ISBN: " + req.getIsbn());
        }
        l.setIsbn(req.getIsbn());
        l.setTitulo(req.getTitulo());
        l.setAutor(req.getAutor());
        l.setCategoria(req.getCategoria());
        l.setStockTotal(req.getStockTotal());
        l.setStockDisponible(req.getStockDisponible());
        return LibroResponse.from(l);
    }

    @Transactional
    public void eliminar(Long id) {
        Libro l = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado: " + id));
        repository.delete(l);
    }

    /**
     * Descuenta una copia de forma atómica. Si no hay stock, falla sin modificar nada.
     * El bloqueo pesimista evita que dos préstamos simultáneos tomen la última copia.
     */
    @Transactional
    public LibroResponse descontarStock(Long id) {
        Libro l = repository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado: " + id));
        if (l.getStockDisponible() <= 0) {
            throw new BusinessRuleException("No hay ejemplares disponibles para el libro: " + l.getTitulo());
        }
        l.setStockDisponible(l.getStockDisponible() - 1);
        return LibroResponse.from(l);
    }

    /**
     * Incrementa el stock al devolver un libro. Nunca puede superar el stock total.
     */
    @Transactional
    public LibroResponse incrementarStock(Long id) {
        Libro l = repository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado: " + id));
        if (l.getStockDisponible() >= l.getStockTotal()) {
            throw new BusinessRuleException("El stock disponible no puede superar el stock total");
        }
        l.setStockDisponible(l.getStockDisponible() + 1);
        return LibroResponse.from(l);
    }

    private void validarStock(Integer total, Integer disponible) {
        if (total == null || disponible == null || total < 0 || disponible < 0) {
            throw new BusinessRuleException("El stock no puede ser negativo");
        }
        if (disponible > total) {
            throw new BusinessRuleException("El stock disponible no puede ser mayor que el stock total");
        }
    }
}
