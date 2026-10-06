package com.biblioteca.loanservice;

import com.biblioteca.loanservice.client.LibroClient;
import com.biblioteca.loanservice.client.UsuarioClient;
import com.biblioteca.loanservice.dto.PrestamoRequest;
import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;
import com.biblioteca.loanservice.exception.BusinessRuleException;
import com.biblioteca.loanservice.repository.PrestamoRepository;
import com.biblioteca.loanservice.service.PrestamoService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SancionadosYSeguridadConcurrenciaTest {

    private static final String SECRET = "cambia_este_secreto_jwt_en_produccion_minimo_32_caracteres";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired PrestamoService prestamoService;
    @Autowired PrestamoRepository repository;
    @Autowired MockMvc mockMvc;
    @MockBean UsuarioClient usuarioClient;
    @MockBean LibroClient libroClient;

    private String token(String sub, String rol) {
        return Jwts.builder().subject(sub).claim("userId", 9L).claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void usuarioConPrestamoAtrasadoSeSancionaConcurrentemente() throws Exception {
        // préstamo activo pero vencido
        Prestamo p = new Prestamo();
        p.setUsuarioId(66L);
        p.setLibroId(77L);
        p.setFechaPrestamo(LocalDate.now().minusDays(30));
        p.setFechaDevolucionEsperada(LocalDate.now().minusDays(10));
        p.setEstado(EstadoPrestamo.ACTIVO);
        repository.saveAndFlush(p);

        Mockito.when(usuarioClient.obtenerUsuario(66L))
                .thenReturn(Map.of("id", 66, "estado", "ACTIVO"));
        Mockito.when(libroClient.obtenerLibro(Mockito.anyLong()))
                .thenAnswer(inv -> Map.of("id", inv.getArgument(0)));

        int hilos = 6;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            final long libro = 2000 + i;
            futures.add(pool.submit(() -> {
                try {
                    salida.await();
                    PrestamoRequest req = new PrestamoRequest();
                    req.setLibroId(libro);
                    req.setUsuarioId(66L);
                    prestamoService.crear(req, 1L, "BIBLIOTECARIO");
                } catch (BusinessRuleException e) {
                    rechazados.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        salida.countDown();
        for (Future<?> f : futures) f.get(90, TimeUnit.SECONDS);
        pool.shutdownNow();

        long nuevos = repository.findByUsuarioIdOrderByFechaPrestamoDesc(66L).size();
        System.out.println("rechazados=" + rechazados.get() + " prestamosTotales=" + nuevos);

        assertThat(rechazados.get()).isEqualTo(hilos);
        // sigue existiendo solo el préstamo vencido original, ahora ATRASADO
        assertThat(nuevos).isEqualTo(1);
        assertThat(repository.findByUsuarioIdOrderByFechaPrestamoDesc(66L).get(0).getEstado())
                .isEqualTo(EstadoPrestamo.ATRASADO);
        Mockito.verify(usuarioClient, Mockito.atLeastOnce()).sancionar(66L);
    }

    @Test
    void seguridadBajoConcurrencia() throws Exception {
        int hilos = 12;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger rechazosCorrectos = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            futures.add(pool.submit(() -> {
                try {
                    salida.await();
                    int s = mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                                    .header("Authorization", "Bearer basura.invalida.jwt"))
                            .andReturn().getResponse().getStatus();
                    if (s == 403 || s == 401) rechazosCorrectos.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }
        for (int i = 0; i < 4; i++) {
            futures.add(pool.submit(() -> {
                try {
                    salida.await();
                    int s = mockMvc.perform(get("/api/v1/prestamos/mis-prestamos"))
                            .andReturn().getResponse().getStatus();
                    if (s == 403 || s == 401) rechazosCorrectos.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }
        for (int i = 0; i < 4; i++) {
            futures.add(pool.submit(() -> {
                try {
                    salida.await();
                    int s = mockMvc.perform(post("/api/v1/prestamos")
                                    .header("Authorization", "Bearer " + token("lector@test.com", "LECTOR"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"libroId\":1}"))
                            .andReturn().getResponse().getStatus();
                    if (s == 403) rechazosCorrectos.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }
        salida.countDown();
        for (Future<?> f : futures) f.get(90, TimeUnit.SECONDS);
        pool.shutdownNow();

        System.out.println("rechazosCorrectos=" + rechazosCorrectos.get() + "/" + hilos);
        assertThat(rechazosCorrectos.get()).isEqualTo(hilos);
        // ningún préstamo fue creado
        assertThat(repository.findAll().size()).isLessThanOrEqualTo(1);
    }
}
