package com.biblioteca.catalogservice.controller;

import com.biblioteca.catalogservice.dto.LibroRequest;
import com.biblioteca.catalogservice.dto.LibroResponse;
import com.biblioteca.catalogservice.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/libros")
public class LibroController {

    private final LibroService service;

    public LibroController(LibroService service) {
        this.service = service;
    }

    @GetMapping
    public List<LibroResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public LibroResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<LibroResponse> crear(@Valid @RequestBody LibroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public LibroResponse actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Endpoints internos de stock (uso entre microservicios) ----

    @GetMapping("/interno/{id}")
    public LibroResponse obtenerInterno(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/stock/descontar")
    public LibroResponse descontar(@PathVariable Long id) {
        return service.descontarStock(id);
    }

    @PostMapping("/{id}/stock/incrementar")
    public LibroResponse incrementar(@PathVariable Long id) {
        return service.incrementarStock(id);
    }
}
