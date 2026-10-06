# Sistema de Gestion de Biblioteca

Backend para la gestion de una biblioteca: catalogo de libros, usuarios, autenticacion JWT, prestamos, devoluciones, sanciones y control de stock.

## Arquitectura

Microservicios con Spring Boot 3 (Java 21). API Gateway como punto de entrada, PostgreSQL con **una base de datos por servicio**.

| Servicio | Puerto | BD | Responsabilidad |
|---|---|---|---|
| api-gateway | 8080 (8095 en equipos con Apache en 8080) | - | Enrutamiento `/api/v1/**`, CORS, health |
| auth-service | 8081 | auth_db | Register, login, emision de JWT |
| user-service | 8082 | user_db | Usuarios, roles, estados, admin inicial |
| catalog-service | 8083 | catalog_db | Libros, ISBN unico, stock atomico |
| loan-service | 8084 | loan_db | Prestamos, devoluciones, reglas de negocio |

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL (local en 5432; las 4 BD ya creadas)
- Docker solo si se usa para levantar PostgreSQL

## Bases de datos

```
auth_db    localhost:5432
user_db    localhost:5432
catalog_db localhost:5432
loan_db    localhost:5432
```

Cada servicio usa **exclusivamente** su propia BD y sus propias tablas (administradas por Flyway). No se comparten tablas entre servicios.

## Configuracion

Variables de entorno (valores por defecto entre llaves en `application.yml`):

```
DB_USERNAME=postgres
DB_PASSWORD=admin
AUTH_DB_URL=jdbc:postgresql://localhost:5432/auth_db
USER_DB_URL=jdbc:postgresql://localhost:5432/user_db
CATALOG_DB_URL=jdbc:postgresql://localhost:5432/catalog_db
LOAN_DB_URL=jdbc:postgresql://localhost:5432/loan_db
JWT_SECRET=<secreto de al menos 32 caracteres>
USER_SERVICE_URL=http://localhost:8082
CATALOG_SERVICE_URL=http://localhost:8083
AUTH_SERVICE_URL=http://localhost:8081
LOAN_SERVICE_URL=http://localhost:8084
```

Ver `.env.example`.

## Ejecutar

```powershell
mvn clean verify
mvn -pl user-service spring-boot:run
mvn -pl catalog-service spring-boot:run
mvn -pl loan-service spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

O ejecutar los jars generados: `java -jar <modulo>/target/<modulo>-0.0.1-SNAPSHOT.jar`.

Gateway en este equipo: `java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar --server.port=8095`.

## Autenticacion

- `POST /api/v1/auth/register` publico, siempre crea rol `LECTOR`.
- `POST /api/v1/auth/login` publico, devuelve JWT con `userId`, `rol`, `sub=email`.
- Todo endpoint protegido usa `Authorization: Bearer <token>`.
- Contrasenas con BCrypt.

Admin inicial: `admin@biblioteca.com` / `Admin123*` (solo desarrollo).

## Roles y permisos

| Endpoint | Permiso |
|---|---|
| GET /api/v1/libros, /{id} | autenticado |
| POST/PUT/DELETE /api/v1/libros/** | ADMIN |
| POST /api/v1/prestamos | ADMIN, BIBLIOTECARIO |
| PATCH /api/v1/prestamos/{id}/devolucion | ADMIN, BIBLIOTECARIO |
| GET /api/v1/prestamos/mis-prestamos | LECTOR (solo propios) |
| GET /api/v1/prestamos/atrasados | ADMIN, BIBLIOTECARIO |

## Reglas de negocio

- Fecha esperada = fecha de prestamo + 14 dias (calculada, no editable).
- Maximo 3 prestamos activos por LECTOR.
- Usuario con prestamo atrasado pasa a SANCIONADO y no puede pedir nuevos.
- Sin stock no hay prestamo; el stock nunca es negativo ni mayor que `stockTotal`.
- Devolucion: fecha real, estado DEVUELTO, stock +1; devolucion duplicada rechazada.
- Concurrencia: `PESSIMISTIC_WRITE` en fila del libro + advisory lock por usuario en `pg_advisory_xact_lock`.

## Swagger / OpenAPI

- auth: http://localhost:8081/swagger-ui.html
- usuarios: http://localhost:8082/swagger-ui.html
- catalogo: http://localhost:8083/swagger-ui.html
- prestamos: http://localhost:8084/swagger-ui.html

## Postman

Coleccion en `docs/postman/SistemaGestionBiblioteca.postman_collection.json`. Importar en Postman, definir `baseUrl` (por defecto `http://localhost:8095`), ejecutar Login ADMIN y Login LECTOR para guardar tokens automaticamente.

## Estructura

```
sistema-gestion-biblioteca/
  pom.xml (agregador)
  api-gateway/
  auth-service/
  user-service/
  catalog-service/
  loan-service/
  docs/
    arquitectura.md
    postman/
  docker-compose.yml
  .env.example
```

Cada modulo es un proyecto Spring Boot independiente (parent `spring-boot-starter-parent`), con su propio `pom.xml`, configuracion, codigo, pruebas y Dockerfile.

## Pruebas

`mvn clean verify`. Incluye pruebas unitarias, de autorizacion (403 por rol), de reglas de negocio y de concurrencia con Testcontainers PostgreSQL.

## Ramas

- `main` (estable, protegida)
- `develop` (integracion, protegida)
- `hgarcia-2023541` (rama de trabajo -> PR a develop)
