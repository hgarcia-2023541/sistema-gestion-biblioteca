package com.biblioteca.loanservice.service;

import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;
import com.biblioteca.loanservice.repository.PrestamoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Marca como ATRASADO los préstamos vencidos en una transacción propia (REQUIRES_NEW)
 * para que el cambio persista aunque la transacción principal falle después.
 */
@Component
public class PrestamoEstadoService {

    private final PrestamoRepository repository;

    public PrestamoEstadoService(PrestamoRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarVencidos(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        List<Prestamo> vencidos = repository
                .findByUsuarioIdAndEstadoAndFechaDevolucionEsperadaBefore(usuarioId, EstadoPrestamo.ACTIVO, hoy);
        vencidos.forEach(p -> p.setEstado(EstadoPrestamo.ATRASADO));
        repository.saveAll(vencidos);
    }
}
