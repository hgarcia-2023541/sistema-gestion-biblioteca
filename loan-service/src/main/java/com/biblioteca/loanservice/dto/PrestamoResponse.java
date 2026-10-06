package com.biblioteca.loanservice.dto;

import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;

import java.time.LocalDate;

public class PrestamoResponse {
    private Long id;
    private Long usuarioId;
    private Long libroId;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucionEsperada;
    private LocalDate fechaDevolucionReal;
    private EstadoPrestamo estado;

    public static PrestamoResponse from(Prestamo p) {
        PrestamoResponse r = new PrestamoResponse();
        r.id = p.getId();
        r.usuarioId = p.getUsuarioId();
        r.libroId = p.getLibroId();
        r.fechaPrestamo = p.getFechaPrestamo();
        r.fechaDevolucionEsperada = p.getFechaDevolucionEsperada();
        r.fechaDevolucionReal = p.getFechaDevolucionReal();
        r.estado = p.getEstado();
        return r;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getLibroId() { return libroId; }
    public LocalDate getFechaPrestamo() { return fechaPrestamo; }
    public LocalDate getFechaDevolucionEsperada() { return fechaDevolucionEsperada; }
    public LocalDate getFechaDevolucionReal() { return fechaDevolucionReal; }
    public EstadoPrestamo getEstado() { return estado; }
}
