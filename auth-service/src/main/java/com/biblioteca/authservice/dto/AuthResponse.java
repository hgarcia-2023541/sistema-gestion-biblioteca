package com.biblioteca.authservice.dto;

public class AuthResponse {
    private String token;
    private String email;
    private String rol;
    private Long userId;

    public AuthResponse(String token, String email, String rol, Long userId) {
        this.token = token;
        this.email = email;
        this.rol = rol;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getRol() { return rol; }
    public Long getUserId() { return userId; }
}
