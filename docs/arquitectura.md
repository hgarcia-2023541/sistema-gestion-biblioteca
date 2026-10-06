# Arquitectura propuesta

## Visión general

Arquitectura de microservicios con un API Gateway como único punto de entrada. Cada servicio posee su propia base de datos (database-per-service) para garantizar aislamiento.

```
Cliente (Postman/Swagger)
        │
        ▼
  api-gateway :8080
   ┌────┬──────┬─────────┐
   ▼    ▼      ▼         ▼
auth  user  catalog   loan
8081  8082   8083     8084
   │    │      │         │
   ▼    ▼      ▼         ▼
auth_db user_db catalog_db loan_db   (PostgreSQL 16)
```

## Decisiones técnicas

1. **Database per service**: cada microservicio tiene su propia base de datos. Se aceptará que `loan-service` consulte datos de usuarios/libros vía API o eventos; no comparte tablas.
2. **JWT stateless**: el token contiene `userId`, `email` y `rol`; cada servicio valida el JWT de forma independiente (sin estado en servidor).
3. **Gateway sin lógica de negocio**: solo enrutamiento y CORS.
4. **Reglas de negocio centralizadas** en los servicios correspondientes (p. ej. sanciones y límites de préstamos en `loan-service`).
5. **Flyway** para migraciones versionadas por servicio (a partir de la Fase 1).
6. **Concurrencia en préstamos**: bloqueo pesimista/optimista sobre `stockDisponible` para evitar sobreventa del último ejemplar.

## Flujo de autenticación

1. `POST /api/auth/login` con email/password.
2. `auth-service` valida credenciales (BCrypt) y emite JWT.
3. El cliente envía `Authorization: Bearer <token>`.
4. Cada servicio valida firma y expiración y aplica autorización por rol.

## Flujo de préstamo

1. BIBLIOTECARIO/ADMIN registra préstamo (lector, libro).
2. `loan-service` verifica: lector activo (no sancionado), máximo 3 préstamos activos, stock > 0.
3. Si el lector tiene un préstamo atrasado → pasa a SANCIONADO y se rechaza.
4. Se decrementa `stockDisponible` de forma transaccional y se calcula fechaEsperada = fechaPrestamo + 14 días.

## Flujo de devolución

1. Se registra `fechaDevolucionReal`, estado `DEVUELTO`, se incrementa stock (≤ stockTotal), en una sola transacción.
