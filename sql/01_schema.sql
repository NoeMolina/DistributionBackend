-- ============================================================
-- Script de Base de Datos: Proyecto Distribución
-- SQL Server
-- ============================================================
CREATE DATABASE DistribucionDB;
GO
USE DistribucionDB;
GO

-- ============================================================
-- TABLA: Articulos
-- ============================================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Articulos' AND xtype='U')
BEGIN
    CREATE TABLE Articulos (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        sku         VARCHAR(50)  NOT NULL UNIQUE,
        descripcion VARCHAR(255) NOT NULL,
        familia     VARCHAR(100) NOT NULL,
        activo      BIT          NOT NULL DEFAULT 1,
        created_at  DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at  DATETIME2    NOT NULL DEFAULT GETDATE()
    );
END
GO

-- ============================================================
-- TABLA: PedidosDistribucion
-- ============================================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='PedidosDistribucion' AND xtype='U')
BEGIN
    CREATE TABLE PedidosDistribucion (
        id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
        articulo_id         BIGINT       NOT NULL,
        fecha_distribucion  DATE         NOT NULL,
        tienda_destino      VARCHAR(150) NOT NULL,
        cantidad_piezas     INT          NOT NULL CHECK (cantidad_piezas > 0),
        estatus             VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE',
        idempotency_key     VARCHAR(100) NULL UNIQUE,
        created_at          DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at          DATETIME2    NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_Pedido_Articulo FOREIGN KEY (articulo_id)
            REFERENCES Articulos(id)
            ON DELETE CASCADE,

        CONSTRAINT CHK_Estatus CHECK (
            estatus IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')
        )
    );
END
GO

-- ============================================================
-- STORED PROCEDURE: sp_CrearPedidoDistribucion
-- ============================================================
CREATE OR ALTER PROCEDURE sp_CrearPedidoDistribucion
    @articuloId        BIGINT,
    @fechaDistribucion DATE,
    @tiendaDestino     VARCHAR(150),
    @cantidadPiezas    INT,
    @estatus           VARCHAR(50),
    @idempotencyKey    VARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF @idempotencyKey IS NOT NULL
    BEGIN
        IF EXISTS (
            SELECT 1 FROM PedidosDistribucion
            WHERE idempotency_key = @idempotencyKey
        )
        BEGIN
            SELECT
                p.id,
                p.articulo_id,
                p.fecha_distribucion,
                p.tienda_destino,
                p.cantidad_piezas,
                p.estatus,
                p.idempotency_key,
                p.created_at,
                p.updated_at,
                'EXISTING' AS resultado
            FROM PedidosDistribucion p
            WHERE p.idempotency_key = @idempotencyKey;
            RETURN;
        END
    END

    IF NOT EXISTS (
        SELECT 1 FROM Articulos
        WHERE id = @articuloId AND activo = 1
    )
    BEGIN
        RAISERROR('El artículo con ID %d no existe o está inactivo.', 16, 1, @articuloId);
        RETURN;
    END

    IF @cantidadPiezas <= 0
    BEGIN
        RAISERROR('La cantidad de piezas debe ser mayor a 0.', 16, 1);
        RETURN;
    END

    IF @estatus NOT IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')
    BEGIN
        RAISERROR('Estatus inválido. Valores permitidos: PENDIENTE, EN_PROCESO, COMPLETADO, CANCELADO.', 16, 1);
        RETURN;
    END

    INSERT INTO PedidosDistribucion (
        articulo_id, fecha_distribucion, tienda_destino,
        cantidad_piezas, estatus, idempotency_key
    )
    VALUES (
        @articuloId, @fechaDistribucion, @tiendaDestino,
        @cantidadPiezas, @estatus, @idempotencyKey
    );

    SELECT
        p.id,
        p.articulo_id,
        p.fecha_distribucion,
        p.tienda_destino,
        p.cantidad_piezas,
        p.estatus,
        p.idempotency_key,
        p.created_at,
        p.updated_at,
        'CREATED' AS resultado
    FROM PedidosDistribucion p
    WHERE p.id = SCOPE_IDENTITY();
END
GO

-- ============================================================
-- STORED PROCEDURE: sp_BuscarPedidosConArticulo
-- ============================================================
CREATE OR ALTER PROCEDURE sp_BuscarPedidosConArticulo
    @sku            VARCHAR(50)  = NULL,
    @familia        VARCHAR(100) = NULL,
    @tiendaDestino  VARCHAR(150) = NULL,
    @estatus        VARCHAR(50)  = NULL,
    @fechaDesde     DATE         = NULL,
    @fechaHasta     DATE         = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        a.id           AS articuloId,
        a.sku,
        a.descripcion  AS articuloDescripcion,
        a.familia,
        p.id           AS pedidoId,
        p.fecha_distribucion,
        p.tienda_destino,
        p.cantidad_piezas,
        p.estatus,
        p.created_at   AS pedidoFechaCreacion
    FROM Articulos a
    INNER JOIN PedidosDistribucion p ON a.id = p.articulo_id
    WHERE
        (@sku           IS NULL OR a.sku            LIKE '%' + @sku + '%')
        AND (@familia       IS NULL OR a.familia        LIKE '%' + @familia + '%')
        AND (@tiendaDestino IS NULL OR p.tienda_destino LIKE '%' + @tiendaDestino + '%')
        AND (@estatus       IS NULL OR p.estatus        = @estatus)
        AND (@fechaDesde    IS NULL OR p.fecha_distribucion >= @fechaDesde)
        AND (@fechaHasta    IS NULL OR p.fecha_distribucion <= @fechaHasta)
    ORDER BY a.sku, p.fecha_distribucion;
END
GO

-- ============================================================
-- Datos de prueba
-- ============================================================
INSERT INTO Articulos (sku, descripcion, familia) VALUES
    ('SKU-001', 'Camiseta Básica Blanca Talla M', 'Ropa'),
    ('SKU-002', 'Pantalón Mezclilla Slim Talla 32', 'Ropa'),
    ('SKU-003', 'Zapato Deportivo Negro Talla 27', 'Calzado'),
    ('SKU-004', 'Mochila Escolar Azul 20L', 'Accesorios');
GO

INSERT INTO PedidosDistribucion (articulo_id, fecha_distribucion, tienda_destino, cantidad_piezas, estatus) VALUES
    (1, '2025-07-10', 'Tienda Centro - CDMX',       50, 'PENDIENTE'),
    (1, '2025-07-15', 'Tienda Polanco - CDMX',      30, 'EN_PROCESO'),
    (2, '2025-07-12', 'Tienda Guadalajara Norte',   25, 'PENDIENTE'),
    (3, '2025-07-20', 'Tienda Monterrey Centro',    40, 'COMPLETADO'),
    (4, '2025-07-18', 'Tienda Puebla Angelópolis',  60, 'PENDIENTE');
GO
