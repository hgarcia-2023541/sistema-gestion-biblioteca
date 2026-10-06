package com.biblioteca.loanservice;

import com.biblioteca.loanservice.client.LibroClient;
import com.biblioteca.loanservice.client.UsuarioClient;
import com.biblioteca.loanservice.dto.PrestamoRequest;
import com.biblioteca.loanservice.dto.PrestamoResponse;
import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;
import com.biblioteca.loanservice.exception.BusinessRuleException;
import com.biblioteca.loanservice.repository.PrestamoRepository;
import com.biblioteca.loanservice.service.PrestamoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock PrestamoRepository repository;
    @Mock UsuarioClient usuarioClient;
    @Mock LibroClient libroClient;
    @Mock EntityManager entityManager;
    @Mock Query lockQuery;
    @Mock com.biblioteca.loanservice.service.PrestamoEstadoService prestamoEstadoService;

    @InjectMocks PrestamoService service;

    @BeforeEach
    void lock() {
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
        lenient().when(lockQuery.setParameter(anyString(), anyLong())).thenReturn(lockQuery);
        lenient().when(lockQuery.getSingleResult()).thenReturn(null);
    }

    private PrestamoRequest req(Long libroId) {
        PrestamoRequest r = new PrestamoRequest();
        r.setLibroId(libroId);
        return r;
    }

    @Test
    void lectorNoPuedeSolicitarParaOtroUsuario() {
        PrestamoRequest r = req(1L);
        r.setUsuarioId(999L);
        assertThrows(BusinessRuleException.class, () -> service.crear(r, 7L, "LECTOR"));
    }

    @Test
    void usuarioSancionadoNoPuedePrestar() {
        when(usuarioClient.obtenerUsuario(1L)).thenReturn(Map.of("id", 1, "estado", "SANCIONADO"));
        assertThrows(BusinessRuleException.class, () -> service.crear(req(1L), 1L, "LECTOR"));
    }

    @Test
    void lectorCon3ActivosNoPuedePrestar() {
        when(usuarioClient.obtenerUsuario(1L)).thenReturn(Map.of("id", 1, "estado", "ACTIVO"));
        when(repository.countByUsuarioIdAndEstado(anyLong(), any())).thenReturn(0L);
        when(repository.countByUsuarioIdAndEstado(1L, EstadoPrestamo.ACTIVO)).thenReturn(3L);
        assertThrows(BusinessRuleException.class, () -> service.crear(req(1L), 1L, "LECTOR"));
    }

    @Test
    void prestamoExitosoCalcula14Dias() {
        when(usuarioClient.obtenerUsuario(1L)).thenReturn(Map.of("id", 1, "estado", "ACTIVO"));
        when(repository.countByUsuarioIdAndEstado(anyLong(), any())).thenReturn(0L);
        when(libroClient.obtenerLibro(2L)).thenReturn(Map.of("id", 2));
        when(repository.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoResponse res = service.crear(req(2L), 1L, "LECTOR");

        assertEquals(EstadoPrestamo.ACTIVO, res.getEstado());
        assertEquals(LocalDate.now().plusDays(14), res.getFechaDevolucionEsperada());
        verify(libroClient).descontarStock(2L);
    }

    @Test
    void devolucionDobleFalla() {
        Prestamo p = new Prestamo();
        p.setEstado(EstadoPrestamo.DEVUELTO);
        when(repository.findWithLockById(5L)).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleException.class, () -> service.devolver(5L));
        verify(libroClient, never()).incrementarStock(anyLong());
    }

    @Test
    void devolucionExitosa() {
        Prestamo p = new Prestamo();
        p.setEstado(EstadoPrestamo.ACTIVO);
        p.setLibroId(8L);
        when(repository.findWithLockById(5L)).thenReturn(Optional.of(p));
        PrestamoResponse res = service.devolver(5L);
        assertEquals(EstadoPrestamo.DEVUELTO, res.getEstado());
        assertEquals(LocalDate.now(), res.getFechaDevolucionReal());
        verify(libroClient).incrementarStock(8L);
    }
}
