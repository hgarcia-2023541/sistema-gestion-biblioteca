package com.biblioteca.loanservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class UsuarioClient {

    private final RestClient restClient;

    public UsuarioClient(@Value("${usuario.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerUsuario(Long id) {
        return restClient.get().uri("/api/v1/usuarios/interno/{id}", id).retrieve().body(Map.class);
    }

    public void sancionar(Long id) {
        restClient.patch().uri("/api/v1/usuarios/interno/{id}/sancionar", id).retrieve().toBodilessEntity();
    }
}
