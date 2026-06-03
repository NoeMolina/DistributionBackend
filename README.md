# Backend de Distribución

API REST construida con Spring Boot, JPA y SQL Server para administrar artículos, tiendas, pedidos de distribución y autenticación JWT.

## Tecnologías

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Security + JWT
- SQL Server

## Requisitos

- Java 21 o superior
- Maven 3.9+
- SQL Server disponible en `localhost:1433`

## Configuración

La conexión a base de datos y JWT se configuran en [src/main/resources/application.yml](src/main/resources/application.yml).

Valores relevantes por defecto:

- Base de datos: `DistribucionDB`
- Usuario SQL Server: `sa`
- Password SQL Server: `MiPassword123!`
- Puerto backend: `8080`
- Secret JWT: definido en `app.security.jwt.secret`

## Base de datos

El script principal está en [sql/01_schema.sql](sql/01_schema.sql).

Incluye:

- Tablas `Articulos`, `Tiendas`, `PedidosDistribucion`, `Usuarios`, `Roles`, `Usuario_Roles` y `Refresh_Tokens`
- Auditoría con `usuario_creacion`, `usuario_modificacion`, `created_at` y `updated_at`
- Idempotencia por clave natural en pedidos: `articulo_id + tienda_id + fecha_distribucion`
- Stored procedures para crear y consultar pedidos
- Índices recomendados para búsquedas frecuentes

## Estructura del proyecto

```text
.
├── pom.xml
├── README.md
├── sql/
│   └── 01_schema.sql
└── src/
    ├── main/
    │   ├── java/com/pruebatecnica/distribucion/
    │   │   ├── DistribucionApplication.java
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   └── service/
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
```

## Ejecución local

1. Ejecuta el script SQL en SQL Server.
2. Verifica que la base `DistribucionDB` exista.
3. Arranca la aplicación:

```bash
mvn spring-boot:run
```

## Endpoints

### Salud y prueba

- `GET /api/test` - endpoint simple para verificar que el backend responde.

### Autenticación

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### Artículos

- `GET /api/articulos` - obtiene el catálogo.
- `GET /api/articulos/{id}` - obtiene un artículo con sus pedidos.
- `POST /api/articulos` - crea un artículo.
- `PUT /api/articulos/{id}` - actualiza un artículo.
- `DELETE /api/articulos/{id}` - elimina lógicamente un artículo y cancela sus pedidos pendientes según la lógica de negocio.

### Pedidos

- `POST /api/articulos/{id}/pedidos` - crea un pedido para un artículo.
- `PUT /api/pedidos/{id}` - actualiza un pedido.
- `DELETE /api/pedidos/{id}` - elimina un pedido.

## Autenticación JWT

El backend usa JWT stateless.

- `login` devuelve `accessToken` y `refreshToken`.
- El `accessToken` se envía en el header:

```http
Authorization: Bearer <token>
```

- `refreshToken` se almacena en la tabla `Refresh_Tokens` como hash.
- El backend incluye handlers globales para responder con JSON en `401`, `403`, `404`, `409` y validaciones.

## Usuarios demo

Al arrancar, el proyecto crea usuarios base para pruebas:

- `admin` / `Admin123*`
- `operador1` / `Operador123*`

## Ejemplos rápidos

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
    "login": "admin",
    "password": "Admin123*"
}
```

### Crear artículo

```http
POST /api/articulos
Authorization: Bearer <token>
Content-Type: application/json

{
    "sku": "SKU-100",
    "descripcion": "Playera tecnica negra",
    "familia": "Ropa",
    "activo": true
}
```

### Crear pedido

```http
POST /api/articulos/1/pedidos
Authorization: Bearer <token>
Content-Type: application/json

{
    "tiendaId": 1,
    "fechaDistribucion": "2026-06-05",
    "cantidadPiezas": 50,
    "estatus": "PENDIENTE"
}
```