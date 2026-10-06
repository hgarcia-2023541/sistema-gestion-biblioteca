package com.biblioteca.loanservice.repository;

import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    long countByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDesc(Long usuarioId);

    List<Prestamo> findByEstadoAndFechaDevolucionEsperadaBefore(EstadoPrestamo estado, LocalDate fecha);

    List<Prestamo> findByUsuarioIdAndEstadoAndFechaDevolucionEsperadaBefore(Long usuarioId, EstadoPrestamo estado, LocalDate fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Prestamo> findWithLockById(Long id);
}
