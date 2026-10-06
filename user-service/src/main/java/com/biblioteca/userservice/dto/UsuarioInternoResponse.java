package com.biblioteca.userservice.dto;

import com.biblioteca.userservice.entity.EstadoUsuario;
import com.biblioteca.userservice.entity.Rol;
import com.biblioteca.userservice.entity.Usuario;

public class UsuarioInternoResponse {
    private Long id;
    private String nombre;
    private String email;
    private String password;
    private Rol rol;
    private EstadoUsuario estado;

    public static UsuarioInternoResponse from(Usuario u) {
        UsuarioInternoResponse r = new UsuarioInternoResponse();
        r.id = u.getId();
        r.nombre = u.getNombre();
        r.email = u.getEmail();
        r.password = u.getPassword();
        r.rol = u.getRol();
        r.estado = u.getEstado();
        return r;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Rol getRol() { return rol; }
    public EstadoUsuario getEstado() { return estado; }
}
