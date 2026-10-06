package com.biblioteca.loanservice.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClaimExtractor {

    public Claims extraer(HttpServletRequest request) {
        Object claims = request.getAttribute("claims");
        if (claims instanceof Claims c) {
            return c;
        }
        throw new IllegalStateException("No se encontraron claims en la petición");
    }
}
