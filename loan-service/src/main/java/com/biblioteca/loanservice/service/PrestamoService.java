package com.biblioteca.loanservice.service;

import com.biblioteca.loanservice.client.LibroClient;
import com.biblioteca.loanservice.client.UsuarioClient;
import com.biblioteca.loanservice.dto.PrestamoRequest;
import com.biblioteca.loanservice.dto.PrestamoResponse;
import com.biblioteca.loanservice.entity.EstadoPrestamo;
import com.biblioteca.loanservice.entity.Prestamo;
import com.biblioteca.loanservice.exception.BusinessRuleException;
import com.biblioteca.loanservice.exception.ResourceNotFoundException;
import com.biblioteca.loanservice.repository.PrestamoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PrestamoService {

    private static final int MAX_PRESTAMOS_ACTIVOS = 3;
    private static final int DIAS_PRESTAMO = 14;

    private final PrestamoRepository repository;
    private final UsuarioClient usuarioClient;
    private final LibroClient libroClient;
    private final EntityManager entityManager;
    private final PrestamoEstadoService prestamoEstadoService;

    public PrestamoService(PrestamoRepository repository, UsuarioClient usuarioClient,
                           LibroClient libroClient, EntityManager entityManager,
                           PrestamoEstadoService prestamoEstadoService) {
        this.repository = repository;
        this.usuarioClient = usuarioClient;
        this.libroClient = libroClient;
        this.entityManager = entityManager;
        this.prestamoEstadoService = prestamoEstadoService;
    }

    @Transactional
    public PrestamoResponse crear(PrestamoRequest req, Long usuarioIdToken, String rolToken) {
        Long usuarioId = resolverUsuarioId(req.getUsuarioId(), usuarioIdToken, rolToken);

        // Serializa las operaciones de préstamo por usuario (bloqueo asesor de PostgreSQL)
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:k)")
                .setParameter("k", usuarioId)
                .getSingleResult();

        Map<String, Object> usuario = usuarioClient.obtenerUsuario(usuarioId);
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + usuarioId);
        }
        String estado = (String) usuario.get("estado");
        if ("SANCIONADO".equals(estado)) {
            throw new BusinessRuleException("El usuario está sancionado y no puede solicitar préstamos");
        }

        LocalDate hoy = LocalDate.now();
        // Regla: si tiene préstamos vencidos, pasan a ATRASADO (persiste en BD) y
        // se sanciona al usuario rechazando el nuevo préstamo
        prestamoEstadoService.marcarVencidos(usuarioId);
        long atrasados = repository.countByUsuarioIdAndEstado(usuarioId, EstadoPrestamo.ATRASADO);
        if (atrasados > 0) {
            usuarioClient.sancionar(usuarioId);
            throw new BusinessRuleException("El usuario tiene préstamos atrasados. Ha sido sancionado.");
        }

        // Regla 2: máximo 3 préstamos activos
        long activos = repository.countByUsuarioIdAndEstado(usuarioId, EstadoPrestamo.ACTIVO);
        if (activos >= MAX_PRESTAMOS_ACTIVOS) {
            throw new BusinessRuleException("El usuario ya tiene el máximo de " + MAX_PRESTAMOS_ACTIVOS + " préstamos activos");
        }

        // Validar que el libro exista antes de descontar stock
        Map<String, Object> libro = libroClient.obtenerLibro(req.getLibroId());
        if (libro == null) {
            throw new ResourceNotFoundException("Libro no encontrado: " + req.getLibroId());
        }

        // Regla 1: el stock se descuenta de forma atómica y con bloqueo pesimista en catalog-service
        try {
            libroClient.descontarStock(req.getLibroId());
        } catch (Exception e) {
            throw new BusinessRuleException("No hay ejemplares disponibles para el libro solicitado");
        }

        try {
            Prestamo p = new Prestamo();
            p.setUsuarioId(usuarioId);
            p.setLibroId(req.getLibroId());
            p.setFechaPrestamo(hoy);
            p.setFechaDevolucionEsperada(hoy.plusDays(DIAS_PRESTAMO));
            p.setEstado(EstadoPrestamo.ACTIVO);
            return PrestamoResponse.from(repository.save(p));
        } catch (RuntimeException e) {
            // Compensación: si falla el registro del préstamo, se devuelve el stock
            try {
                libroClient.incrementarStock(req.getLibroId());
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    @Transactional
    public PrestamoResponse devolver(Long id) {
        Prestamo p = repository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado: " + id));

        if (p.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessRuleException("El préstamo ya fue devuelto");
        }

        p.setFechaDevolucionReal(LocalDate.now());
        p.setEstado(EstadoPrestamo.DEVUELTO);

        try {
            libroClient.incrementarStock(p.getLibroId());
        } catch (Exception e) {
            throw new BusinessRuleException("No se pudo incrementar el stock del libro");
        }
        return PrestamoResponse.from(p);
    }

    @Transactional
    public List<PrestamoResponse> misPrestamos(Long usuarioIdToken) {
        marcarAtrasados();
        return repository.findByUsuarioIdOrderByFechaPrestamoDesc(usuarioIdToken).stream()
                .map(PrestamoResponse::from).toList();
    }

    @Transactional
    public List<PrestamoResponse> atrasados() {
        marcarAtrasados();
        return repository.findByEstadoAndFechaDevolucionEsperadaBefore(EstadoPrestamo.ATRASADO, LocalDate.now())
                .stream().map(PrestamoResponse::from).toList();
    }

    private void marcarAtrasados() {
        LocalDate hoy = LocalDate.now();
        repository.findByEstadoAndFechaDevolucionEsperadaBefore(EstadoPrestamo.ACTIVO, hoy)
                .forEach(p -> p.setEstado(EstadoPrestamo.ATRASADO));
    }

    private Long resolverUsuarioId(Long solicitado, Long tokenUsuarioId, String rolToken) {
        if ("LECTOR".equals(rolToken)) {
            if (solicitado != null && !solicitado.equals(tokenUsuarioId)) {
                throw new BusinessRuleException("Un LECTOR solo puede solicitar préstamos para sí mismo");
            }
            return tokenUsuarioId;
        }
        return solicitado != null ? solicitado : tokenUsuarioId;
    }
}
