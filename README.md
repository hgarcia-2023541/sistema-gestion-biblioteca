# Sistema de Gestión de Biblioteca

Backend para la gestión de una biblioteca: catálogo de libros, usuarios, autenticación JWT, préstamos, devoluciones, sanciones y control de stock.

## Arquitectura

Microservicios con Spring Boot 3 (Java 21), comunicación vía API Gateway, PostgreSQL como base de datos y JWT para seguridad.

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| api-gateway | 8080 | Punto de entrada, enrutamiento, CORS |
| auth-service | 8081 | Login y generación/validación de JWT |
| user-service | 8082 | Gestión de usuarios, roles y estados |
| catalog-service | 8083 | Libros, stock, catálogo |
| loan-service | 8084 | Préstamos, devoluciones, reglas de negocio |

Decisión de datos: **una base de datos por microservicio** (`auth_db`, `user_db`, `catalog_db`, `loan_db`) para respetar el aislamiento de datos propio de microservicios.

## Tecnologías

Java 21, Spring Boot 3.3.x, Spring Security, JWT (jjwt), Spring Data JPA, Hibernate, PostgreSQL 16, Flyway, Maven, JUnit 5, Mockito, Testcontainers, OpenAPI/Swagger, Docker & Docker Compose.

## Requisitos

- Java 21
- Maven 3.9+
- Docker y Docker Compose
- GitHub CLI (para contribuir)

## Configuración

```powershell
copy .env.example .env
# Editar .env con valores reales (nunca commitear .env)
```

## Base de datos con Docker

```powershell
docker compose up -d postgres
```

Esto crea PostgreSQL y las cuatro bases de datos iniciales (`docker/postgres/init-databases.sql`).

## Compilar y probar

```powershell
mvn clean install
mvn test
```

## Estructura

```
sistema-gestion-biblioteca/
├── api-gateway/
├── auth-service/
├── user-service/
├── catalog-service/
├── loan-service/
├── docker/postgres/
├── docs/
├── docker-compose.yml
├── pom.xml
└── .env.example
```

## Ramas

- `main` — estable (protegida)
- `develop` — integración (protegida)
- `hgarcia-2023541` — rama de trabajo

## Documentación adicional

- [docs/arquitectura.md](docs/arquitectura.md)

## Estado

Fase 0 completada: repositorio, estructura base, Docker Compose para PostgreSQL y documentación inicial. Las funcionalidades de negocio se implementan incrementalmente.
