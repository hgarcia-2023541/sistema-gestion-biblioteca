package com.biblioteca.authservice.service;

import com.biblioteca.authservice.client.UsuarioClient;
import com.biblioteca.authservice.dto.AuthResponse;
import com.biblioteca.authservice.dto.LoginRequest;
import com.biblioteca.authservice.dto.RegisterRequest;
import com.biblioteca.authservice.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UsuarioClient usuarioClient;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioClient usuarioClient, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.usuarioClient = usuarioClient;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> register(RegisterRequest req) {
        return usuarioClient.crearLector(req.getNombre(), req.getEmail(), req.getPassword());
    }

    public AuthResponse login(LoginRequest req) {
        Map<String, Object> usuario = usuarioClient.buscarPorEmail(req.getEmail());
        if (usuario == null) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        String hash = (String) usuario.get("password");
        if (!passwordEncoder.matches(req.getPassword(), hash)) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        Long id = ((Number) usuario.get("id")).longValue();
        String rol = (String) usuario.get("rol");
        String token = jwtService.generar(id, req.getEmail(), rol);
        return new AuthResponse(token, req.getEmail(), rol, id);
    }
}
