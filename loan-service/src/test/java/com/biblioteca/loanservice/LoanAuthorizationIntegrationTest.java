package com.biblioteca.loanservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoanAuthorizationIntegrationTest {

    private static final String SECRET = "cambia_este_secreto_jwt_en_produccion_minimo_32_caracteres";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    private String token(String rol) {
        return Jwts.builder()
                .subject(rol.toLowerCase() + "@test.com")
                .claim("userId", 1L)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String auth(String rol) {
        return "Bearer " + token(rol);
    }

    @Test
    void lectorNoPuedeCrearPrestamo() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", auth("LECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libroId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearPrestamo() throws Exception {
        // No debe ser 401/403 (puede ser 4xx/5xx por dependencias no disponibles en el test)
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", auth("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libroId\":1,\"usuarioId\":1}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void bibliotecarioPuedeCrearPrestamo() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", auth("BIBLIOTECARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libroId\":1,\"usuarioId\":1}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void lectorNoPuedeRegistrarDevolucion() throws Exception {
        mockMvc.perform(patch("/api/v1/prestamos/1/devolucion")
                        .header("Authorization", auth("LECTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lectorPuedeConsultarSusPrestamos() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", auth("LECTOR")))
                .andExpect(status().isOk());
    }

    @Test
    void adminPuedeVerAtrasados() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", auth("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void bibliotecarioPuedeVerAtrasados() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", auth("BIBLIOTECARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void sinTokenNoHayAcceso() throws Exception {
        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos"))
                .andExpect(status().isForbidden());
    }
}
