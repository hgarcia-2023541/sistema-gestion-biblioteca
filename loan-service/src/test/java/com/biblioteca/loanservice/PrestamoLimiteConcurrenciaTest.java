package com.biblioteca.loanservice;

import com.biblioteca.loanservice.client.LibroClient;
import com.biblioteca.loanservice.client.UsuarioClient;
import com.biblioteca.loanservice.dto.PrestamoRequest;
import com.biblioteca.loanservice.dto.PrestamoResponse;
import com.biblioteca.loanservice.entity.EstadoPrestamo;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PrestamoLimiteConcurrenciaTest {

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
    void nuncaSuperaTresPrestamosActivos() throws Exception {
        long lectorId = 42L;
        Mockito.when(usuarioClient.obtenerUsuario(lectorId))
                .thenReturn(Map.of("id", lectorId, "estado", "ACTIVO"));
        Mockito.when(libroClient.obtenerLibro(Mockito.anyLong()))
                .thenAnswer(inv -> Map.of("id", inv.getArgument(0)));

        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            final long libro = 1000 + i;
            futures.add(pool.submit(() -> {
                try {
                    salida.await();
                    PrestamoRequest req = new PrestamoRequest();
                    req.setLibroId(libro);
                    req.setUsuarioId(lectorId);
                    PrestamoResponse r = prestamoService.crear(req, 1L, "BIBLIOTECARIO");
                    if (r != null) exitosos.incrementAndGet();
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

        long activos = repository.countByUsuarioIdAndEstado(lectorId, EstadoPrestamo.ACTIVO);
        System.out.println("exitosos=" + exitosos.get() + " rechazados=" + rechazados.get()
                + " activosEnDb=" + activos);

        assertThat(exitosos.get()).isEqualTo(3);
        assertThat(rechazados.get()).isEqualTo(hilos - 3);
        assertThat(activos).isEqualTo(3);
    }
}
