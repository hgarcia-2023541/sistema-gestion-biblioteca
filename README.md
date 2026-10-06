# Sistema de Gestión de Biblioteca

Backend para la gestión de una biblioteca mediante una arquitectura de microservicios. El sistema permite administrar usuarios, libros y préstamos, aplicando autenticación mediante JWT, control de acceso por roles y reglas de negocio para garantizar la integridad de los préstamos y del inventario.

## 1. Descripción del proyecto

El sistema permite:

- Registrar y autenticar usuarios.
- Gestionar usuarios y sus roles.
- Administrar el catálogo de libros.
- Consultar disponibilidad e inventario.
- Registrar préstamos.
- Registrar devoluciones.
- Consultar el historial de préstamos propios.
- Consultar préstamos atrasados.
- Controlar automáticamente el stock disponible.
- Aplicar restricciones según el rol del usuario.
- Gestionar sanciones cuando existen préstamos atrasados.

La aplicación está desarrollada como un conjunto de microservicios independientes que se comunican mediante API REST.

---

## 2. Tecnologías utilizadas

- **Java 21**
- **Spring Boot 3**
- **Spring Security**
- **JWT**
- **Spring Data JPA / Hibernate**
- **PostgreSQL**
- **Flyway**
- **Maven**
- **Jakarta Validation**
- **BCrypt**
- **JUnit 5**
- **Mockito**
- **Spring Boot Test**
- **Testcontainers**
- **Swagger / OpenAPI**
- **Git / GitHub**
- **Postman**

---

## 3. Arquitectura del sistema

El proyecto está compuesto por cinco servicios:

| Servicio | Responsabilidad | Puerto |
|---|---|---:|
| API Gateway | Punto de entrada y enrutamiento de las peticiones | **8095** |
| Auth Service | Registro, autenticación y generación de JWT | 8081 |
| User Service | Gestión de usuarios, roles y estados | 8082 |
| Catalog Service | Gestión de libros y control de inventario | 8083 |
| Loan Service | Gestión de préstamos, devoluciones y reglas de negocio | 8084 |

El acceso externo a la API se realiza mediante el API Gateway:

```text
http://localhost:8095/api/v1
```

El puerto `8095` se utiliza para evitar conflictos con otros servicios locales que utilizan el puerto `8080`.

### Flujo general

```text
Cliente
   |
   v
API Gateway :8095
   |
   +----> Auth Service :8081
   |
   +----> User Service :8082
   |
   +----> Catalog Service :8083
   |
   +----> Loan Service :8084
```

---

## 4. Bases de datos

El sistema utiliza PostgreSQL con una base de datos independiente para cada servicio:

| Servicio | Base de datos |
|---|---|
| Auth Service | `auth_db` |
| User Service | `user_db` |
| Catalog Service | `catalog_db` |
| Loan Service | `loan_db` |

Cada servicio administra únicamente la información correspondiente a su propia base de datos.

Las migraciones iniciales se gestionan mediante **Flyway**.

---

## 5. Roles del sistema

El sistema utiliza tres roles principales:

### ADMIN

Cuenta con control administrativo del sistema.

Puede:

- Gestionar usuarios.
- Crear, modificar y eliminar libros.
- Registrar préstamos.
- Registrar devoluciones.
- Consultar préstamos atrasados.
- Consultar el catálogo.

### BIBLIOTECARIO

Puede realizar las operaciones relacionadas con la gestión de préstamos:

- Consultar libros.
- Registrar préstamos.
- Registrar devoluciones.
- Consultar préstamos atrasados.

### LECTOR

Puede:

- Consultar el catálogo.
- Consultar sus propios préstamos.
- Autenticarse mediante JWT.

El lector no puede realizar operaciones administrativas ni registrar préstamos directamente.

---

## 6. Autenticación y seguridad

La autenticación utiliza **JWT (JSON Web Token)** y la aplicación funciona de manera stateless.

Las contraseñas se almacenan utilizando **BCrypt**.

El flujo de seguridad es:

```text
Login
  |
  v
JWT
  |
  v
Security Filter
  |
  v
Validación de autenticación
  |
  v
Validación de rol
  |
  v
Controller
  |
  v
Service
  |
  v
Repository
```

Los endpoints protegidos requieren un token JWT válido mediante:

```text
Authorization: Bearer <token>
```

---

## 7. Endpoints principales

La API utiliza la siguiente estructura:

### Autenticación

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Libros

```text
GET    /api/v1/libros
GET    /api/v1/libros/{id}
POST   /api/v1/libros
PUT    /api/v1/libros/{id}
DELETE /api/v1/libros/{id}
```

### Préstamos

```text
POST  /api/v1/prestamos
PATCH /api/v1/prestamos/{id}/devolucion
GET   /api/v1/prestamos/mis-prestamos
GET   /api/v1/prestamos/atrasados
```

La documentación interactiva de la API está disponible mediante Swagger/OpenAPI en los servicios correspondientes.

---

## 8. Reglas de negocio

El sistema implementa las siguientes reglas:

- Un libro no puede prestarse cuando su stock disponible es `0`.
- Un lector puede tener como máximo **3 préstamos activos**.
- Cada préstamo tiene un plazo de **14 días**.
- Si un lector tiene un préstamo atrasado, se genera automáticamente una sanción.
- Un usuario sancionado no puede realizar nuevos préstamos.
- Al realizar una devolución, el stock disponible se incrementa.
- Un préstamo no puede devolverse más de una vez.
- El stock disponible nunca puede ser negativo.
- El stock disponible no puede superar el stock total.
- Las operaciones críticas de préstamos y devoluciones utilizan transacciones.
- Se utilizan mecanismos de bloqueo para proteger el stock ante operaciones concurrentes.

---

## 9. Ejecución del proyecto

El proyecto incluye Maven Wrapper, por lo que no es necesario tener Maven instalado globalmente.

Para verificar y compilar todos los módulos:

```powershell
.\mvnw.cmd clean verify
```

Para ejecutar un servicio individual:

```powershell
.\mvnw.cmd -pl api-gateway spring-boot:run
```

Los servicios deben ejecutarse en los puertos configurados anteriormente.

Antes de realizar las pruebas funcionales, es necesario tener PostgreSQL disponible y las bases de datos configuradas.

---

## 10. Pruebas automatizadas

El proyecto cuenta con pruebas unitarias y de integración para validar la funcionalidad principal.

Para ejecutarlas:

```powershell
.\mvnw.cmd clean verify
```

Resultado de la validación realizada:

```text
BUILD SUCCESS
24 pruebas ejecutadas
0 fallos
0 errores
```

Las pruebas incluyen validaciones de:

- Autenticación.
- Autorización por roles.
- Reglas de préstamos.
- Control de stock.
- Préstamos máximos por lector.
- Devoluciones.
- Devoluciones duplicadas.
- Sanciones.
- Concurrencia.
- Persistencia con PostgreSQL.
- Integración entre componentes.

---

## 11. Pruebas funcionales

Las pruebas funcionales fueron realizadas mediante **Postman**, utilizando el API Gateway como punto de entrada.

### Pruebas realizadas

#### Autenticación

- Login de ADMIN.
- Registro de usuario LECTOR.
- Login de LECTOR.
- Validación de JWT.

#### Catálogo

- Consulta de libros.
- Consulta de libro por ID.
- Creación de libros como ADMIN.
- Validación de permisos para crear libros.

#### Préstamos

- Registro de préstamo.
- Validación del stock.
- Consulta de préstamos del lector.
- Registro de devolución.
- Validación del incremento del stock.
- Validación de devolución duplicada.

#### Seguridad

Se comprobó que un usuario con rol LECTOR no puede realizar operaciones administrativas.

Resultado observado:

```text
HTTP 403 Forbidden
```

Esto confirma que el control de acceso por roles está funcionando correctamente.

---

## 12. Prueba de estrés

Para realizar la prueba de estrés se utilizó el script:

```text
test-api1.sh
```

El script ejecuta pruebas funcionales básicas y posteriormente realiza solicitudes concurrentes contra el catálogo.

### Configuración utilizada

```text
Servidor:
http://localhost:8095/api/v1

Solicitudes:
100

Concurrencia:
10 hilos paralelos

Herramienta:
cURL + xargs
```

ApacheBench (`ab`) no se encontraba disponible en el entorno de ejecución, por lo que el script utilizó automáticamente el mecanismo alternativo basado en `cURL` y `xargs`.

### Resultado

```text
100 200
```

Interpretación:

```text
100 solicitudes realizadas
100 respuestas HTTP 200 OK
0 respuestas fallidas en la prueba de estrés
```

El resultado confirma que el API Gateway y el catálogo pudieron atender las 100 solicitudes concurrentes realizadas durante la prueba sin producir errores HTTP.

### Seguridad durante la prueba

El mismo script también verificó que un usuario con rol LECTOR no pudiera crear libros.

Resultado:

```text
403 Forbidden
```

Por lo tanto, la prueba permitió validar tanto el comportamiento bajo carga como el control de acceso básico.

---

## 13. Postman

El proyecto incluye una colección de Postman para facilitar las pruebas de la API:

```text
docs/postman/SistemaGestionBiblioteca.postman_collection.json
```

La colección contiene las operaciones principales de:

- Autenticación.
- Catálogo.
- Préstamos.

También utiliza variables para facilitar las pruebas, incluyendo:

```text
baseUrl
tokenAdmin
tokenLector
userId
libroId
prestamoId
```

La URL base utilizada en el entorno actual es:

```text
http://localhost:8095
```

---

## 14. Documentación Swagger / OpenAPI

Los servicios cuentan con documentación mediante Swagger/OpenAPI.

La documentación permite consultar los endpoints disponibles y realizar pruebas directamente desde la interfaz web.

Los endpoints protegidos utilizan autenticación mediante:

```text
Bearer JWT
```

---

## 15. Estructura general del proyecto

```text
sistema-gestion-biblioteca/
│
├── api-gateway/
├── auth-service/
├── user-service/
├── catalog-service/
├── loan-service/
│
├── docs/
│   └── postman/
│       └── SistemaGestionBiblioteca.postman_collection.json
│
├── test-api1.sh
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## 16. Estado final del proyecto

El backend se encuentra implementado y validado mediante:

- Microservicios independientes.
- PostgreSQL.
- Autenticación JWT.
- Control de acceso mediante roles.
- BCrypt.
- Validaciones de negocio.
- Transacciones.
- Control de concurrencia.
- Migraciones con Flyway.
- DTOs para comunicación de la API.
- Pruebas unitarias y de integración.
- Pruebas funcionales con Postman.
- Prueba de estrés mediante `test-api1.sh`.
- Documentación Swagger/OpenAPI.
- Colección Postman.

### Resultado de las validaciones

```text
Pruebas automatizadas:
24 ejecutadas
0 fallos
0 errores

Prueba de estrés:
100 solicitudes
100 respuestas HTTP 200
```

El proyecto utiliza el API Gateway en el puerto `8095` para las pruebas locales actuales.