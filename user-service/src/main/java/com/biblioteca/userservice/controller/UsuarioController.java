package com.biblioteca.userservice.controller;

import com.biblioteca.userservice.dto.CrearUsuarioRequest;
import com.biblioteca.userservice.dto.UsuarioInternoResponse;
import com.biblioteca.userservice.dto.UsuarioResponse;
import com.biblioteca.userservice.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    // ---- Endpoints internos (uso entre microservicios, red interna) ----

    @PostMapping("/interno")
    public ResponseEntity<UsuarioResponse> crearInterno(@Valid @RequestBody CrearUsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearLector(req));
    }

    @GetMapping("/interno/por-email")
    public UsuarioInternoResponse porEmail(@RequestParam String email) {
        return service.buscarPorEmail(email);
    }

    @GetMapping("/interno/{id}")
    public UsuarioResponse obtenerInterno(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PatchMapping("/interno/{id}/sancionar")
    public UsuarioResponse sancionar(@PathVariable Long id) {
        return service.sancionar(id);
    }
}
