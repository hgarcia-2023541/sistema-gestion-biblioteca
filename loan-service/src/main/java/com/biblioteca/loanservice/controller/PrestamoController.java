package com.biblioteca.loanservice.controller;

import com.biblioteca.loanservice.dto.PrestamoRequest;
import com.biblioteca.loanservice.dto.PrestamoResponse;
import com.biblioteca.loanservice.service.PrestamoService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
public class PrestamoController {

    private final PrestamoService service;
    private final ClaimExtractor claimExtractor;

    public PrestamoController(PrestamoService service, ClaimExtractor claimExtractor) {
        this.service = service;
        this.claimExtractor = claimExtractor;
    }

    @PostMapping
    public ResponseEntity<PrestamoResponse> crear(@Valid @RequestBody PrestamoRequest req, HttpServletRequest httpReq) {
        Claims claims = claimExtractor.extraer(httpReq);
        Long userId = claims.get("userId", Long.class);
        String rol = claims.get("rol", String.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req, userId, rol));
    }

    @PatchMapping("/{id}/devolucion")
    public PrestamoResponse devolver(@PathVariable Long id) {
        return service.devolver(id);
    }

    @GetMapping("/mis-prestamos")
    public List<PrestamoResponse> misPrestamos(HttpServletRequest httpReq) {
        Claims claims = claimExtractor.extraer(httpReq);
        return service.misPrestamos(claims.get("userId", Long.class));
    }

    @GetMapping("/atrasados")
    public List<PrestamoResponse> atrasados() {
        return service.atrasados();
    }
}
