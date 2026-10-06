package com.biblioteca.loanservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class LibroClient {

    private final RestClient restClient;

    public LibroClient(@Value("${libro.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerLibro(Long id) {
        return restClient.get().uri("/api/v1/libros/interno/{id}", id).retrieve().body(Map.class);
    }

    public void descontarStock(Long id) {
        restClient.post().uri("/api/v1/libros/{id}/stock/descontar", id).retrieve().toBodilessEntity();
    }

    public void incrementarStock(Long id) {
        restClient.post().uri("/api/v1/libros/{id}/stock/incrementar", id).retrieve().toBodilessEntity();
    }
}
