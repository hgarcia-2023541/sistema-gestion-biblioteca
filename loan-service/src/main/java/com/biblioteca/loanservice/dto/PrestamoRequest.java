package com.biblioteca.loanservice.dto;

import jakarta.validation.constraints.NotNull;

public class PrestamoRequest {

    @NotNull(message = "El libroId es obligatorio")
    private Long libroId;

    /** Solo ADMIN/BIBLIOTECARIO pueden indicar otro usuario. Para LECTOR se usa el del token. */
    private Long usuarioId;

    public Long getLibroId() { return libroId; }
    public void setLibroId(Long libroId) { this.libroId = libroId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
}
