package com.biblioteca.catalogservice;

import com.biblioteca.catalogservice.entity.Libro;
import com.biblioteca.catalogservice.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LibroStockConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired LibroRepository libroRepository;

    private Long libroId;

    @BeforeEach
    void seed() {
        Libro l = new Libro();
        l.setIsbn("CONC-001");
        l.setTitulo("Última copia");
        l.setAutor("Test");
        l.setCategoria("Test");
        l.setStockTotal(1);
        l.setStockDisponible(1);
        libroId = libroRepository.saveAndFlush(l).getId();
    }

    @Test
    void soloUnPrestamoExitosoConUltimaCopia() throws Exception {
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> resultados = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            resultados.add(pool.submit(() -> {
                try {
                    salida.await();
                    int status = mockMvc.perform(post("/api/v1/libros/{id}/stock/descontar", libroId))
                            .andReturn().getResponse().getStatus();
                    if (status == 200) exitos.incrementAndGet();
                    else rechazados.incrementAndGet();
                } catch (Exception e) {
                    rechazados.incrementAndGet();
                }
            }));
        }
        salida.countDown();
        for (Future<?> f : resultados) f.get(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        Libro final_ = libroRepository.findById(libroId).orElseThrow();
        System.out.println("exitos=" + exitos.get() + " rechazados=" + rechazados.get()
                + " stockFinal=" + final_.getStockDisponible());

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazados.get()).isEqualTo(hilos - 1);
        assertThat(final_.getStockDisponible()).isZero();
        assertThat(final_.getStockDisponible()).isGreaterThanOrEqualTo(0);
    }
}
