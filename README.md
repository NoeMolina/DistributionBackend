# Prueba técnica - Distribución

Base inicial del proyecto con Spring Boot y SQL Server.

## Estructura

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
    │   │   └── service/
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
```

## Siguiente paso sugerido

1. Crear las entidades JPA para `Articulos` y `PedidosDistribucion`.
2. Crear los repositorios.
3. Crear el servicio con la lógica del stored procedure.
4. Exponer los endpoints REST.
