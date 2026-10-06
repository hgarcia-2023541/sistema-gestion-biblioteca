package com.biblioteca.authservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class UsuarioClient {

    private final RestClient restClient;

    public UsuarioClient(@Value("${usuario.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Map<String, Object> buscarPorEmail(String email) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/api/v1/usuarios/interno/por-email").queryParam("email", email).build())
                    .retrieve().body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public Map<String, Object> crearLector(String nombre, String email, String password) {
        return restClient.post()
                .uri("/api/v1/usuarios/interno")
                .body(Map.of("nombre", nombre, "email", email, "password", password))
                .retrieve().body(Map.class);
    }
}
