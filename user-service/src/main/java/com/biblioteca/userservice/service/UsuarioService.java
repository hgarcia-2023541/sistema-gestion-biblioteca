package com.biblioteca.userservice.service;

import com.biblioteca.userservice.dto.CrearUsuarioRequest;
import com.biblioteca.userservice.dto.UsuarioInternoResponse;
import com.biblioteca.userservice.dto.UsuarioResponse;
import com.biblioteca.userservice.entity.EstadoUsuario;
import com.biblioteca.userservice.entity.Rol;
import com.biblioteca.userservice.entity.Usuario;
import com.biblioteca.userservice.exception.BusinessRuleException;
import com.biblioteca.userservice.exception.ResourceNotFoundException;
import com.biblioteca.userservice.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse crearLector(CrearUsuarioRequest req) {
        return crear(req, Rol.LECTOR);
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest req, Rol rol) {
        if (repository.existsByEmail(req.getEmail())) {
            throw new BusinessRuleException("El email ya está registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.getNombre());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRol(rol);
        u.setEstado(EstadoUsuario.ACTIVO);
        return UsuarioResponse.from(repository.save(u));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll().stream().map(UsuarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public UsuarioInternoResponse buscarPorEmail(String email) {
        Usuario u = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        return UsuarioInternoResponse.from(u);
    }

    @Transactional
    public UsuarioResponse sancionar(Long id) {
        Usuario u = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        u.setEstado(EstadoUsuario.SANCIONADO);
        return UsuarioResponse.from(u);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.from(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id)));
    }
}
