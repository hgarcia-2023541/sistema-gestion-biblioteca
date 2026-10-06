package com.biblioteca.loanservice;

import com.biblioteca.loanservice.client.LibroClient;
import com.biblioteca.loanservice.client.UsuarioClient;
import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;
import com.biblioteca.loanservice.exception.BusinessRuleException;
import com.biblioteca.loanservice.repository.PrestamoRepository;
import com.biblioteca.loanservice.service.PrestamoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest
@Testcontainers
class PrestamoDevolucionConcurrenciaTest {

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
    @MockBean UsuarioClient usuarioClient;
    @MockBean LibroClient libroClient;

    @Test
    void soloUnaDevolucionEfectiva() throws Exception {
        Prestamo p = new Prestamo();
        p.setUsuarioId(7L);
        p.setLibroId(55L);
        p.setFechaPrestamo(LocalDate.now());
        p.setFechaDevolucionEsperada(LocalDate.now().plusDays(14));
        p.setEstado(EstadoPrestamo.ACTIVO);
        Long id = repository.saveAndFlush(p).getId();

        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            futures.add(pool.submit(() -> {
                try {
                    salida.await();
                    prestamoService.devolver(id);
                    exitosas.incrementAndGet();
                } catch (BusinessRuleException e) {
                    rechazadas.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        salida.countDown();
        for (Future<?> f : futures) f.get(90, TimeUnit.SECONDS);
        pool.shutdownNow();

        Prestamo final_ = repository.findById(id).orElseThrow();
        System.out.println("devolucionesExitosas=" + exitosas.get() + " rechazos=" + rechazadas.get()
                + " estado=" + final_.getEstado());

        assertThat(exitosas.get()).isEqualTo(1);
        assertThat(rechazadas.get()).isEqualTo(hilos - 1);
        assertThat(final_.getEstado()).isEqualTo(EstadoPrestamo.DEVUELTO);
        assertThat(final_.getFechaDevolucionReal()).isNotNull();
        // incrementarStock en catalog solo puede ser llamado una vez
        Mockito.verify(libroClient, Mockito.times(1)).incrementarStock(55L);
    }
}
