package com.biblioteca.catalogservice.repository;

import com.biblioteca.catalogservice.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Optional<Libro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    /**
     * Bloqueo pesimista de escritura: garantiza que dos solicitudes
     * simultáneas no puedan prestar/devolver el último ejemplar a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Libro> findWithLockById(Long id);
}
