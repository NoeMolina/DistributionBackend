-- ============================================================
-- Script de Base de Datos: Proyecto Distribucion
-- SQL Server - Modelo refactorizado para auditoria e idempotencia
-- ============================================================

IF DB_ID('DistribucionDB') IS NULL
BEGIN
    CREATE DATABASE DistribucionDB;
END
GO

USE DistribucionDB;
GO
 
-- ============================================================
-- Limpieza controlada para reprovisionar el esquema de la prueba
-- ============================================================
IF OBJECT_ID('sp_CrearPedidoDistribucion', 'P') IS NOT NULL
    DROP PROCEDURE sp_CrearPedidoDistribucion;
GO
IF OBJECT_ID('sp_BuscarPedidosConArticulo', 'P') IS NOT NULL
    DROP PROCEDURE sp_BuscarPedidosConArticulo;
GO

IF OBJECT_ID('Refresh_Tokens', 'U') IS NOT NULL DROP TABLE Refresh_Tokens;
IF OBJECT_ID('Usuario_Roles', 'U') IS NOT NULL DROP TABLE Usuario_Roles;
IF OBJECT_ID('PedidosDistribucion', 'U') IS NOT NULL DROP TABLE PedidosDistribucion;
IF OBJECT_ID('Roles', 'U') IS NOT NULL DROP TABLE Roles;
IF OBJECT_ID('Usuarios', 'U') IS NOT NULL DROP TABLE Usuarios;
IF OBJECT_ID('Tiendas', 'U') IS NOT NULL DROP TABLE Tiendas;
IF OBJECT_ID('Articulos', 'U') IS NOT NULL DROP TABLE Articulos;
GO

-- ============================================================
-- TABLA: Articulos
-- Nota de diseno: se mantiene UNIQUE de SKU por ser clave natural de catalogo.
-- ============================================================
CREATE TABLE Articulos (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    sku                   VARCHAR(50)  NOT NULL UNIQUE,
    descripcion           VARCHAR(255) NOT NULL,
    familia               VARCHAR(100) NOT NULL,
    activo                BIT          NOT NULL DEFAULT 1,
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
-- TABLA: Tiendas
-- Nota de diseno: se mantiene UNIQUE de codigo para identificar tienda de negocio.
-- ============================================================
CREATE TABLE Tiendas (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    codigo                VARCHAR(50)  NOT NULL UNIQUE,
    nombre                VARCHAR(150) NOT NULL,
    ubicacion             VARCHAR(255) NULL,
    activa                BIT          NOT NULL DEFAULT 1,
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
-- TABLA: Usuarios
-- ============================================================
CREATE TABLE Usuarios (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    username              VARCHAR(80)  NOT NULL UNIQUE,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    activo                BIT          NOT NULL DEFAULT 1,
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
-- TABLA: Roles
-- ============================================================
CREATE TABLE Roles (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    codigo                VARCHAR(50)  NOT NULL UNIQUE,
    nombre                VARCHAR(100) NOT NULL,
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE()
);
GO

-- ============================================================
-- TABLA: PedidosDistribucion
-- Idempotencia por regla de negocio:
--   UNIQUE (articulo_id, tienda_id, fecha_distribucion)
-- Esto reemplaza completamente idempotency_key.
-- ============================================================
CREATE TABLE PedidosDistribucion (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    articulo_id           BIGINT       NOT NULL,
    tienda_id             BIGINT       NOT NULL,
    fecha_distribucion    DATE         NOT NULL,
    cantidad_piezas       INT          NOT NULL,
    estatus               VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE',
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_Pedido_Articulo FOREIGN KEY (articulo_id)
        REFERENCES Articulos(id)
        ON DELETE CASCADE,

    CONSTRAINT FK_Pedido_Tienda FOREIGN KEY (tienda_id)
        REFERENCES Tiendas(id)
        ON DELETE NO ACTION,

    CONSTRAINT CHK_CantidadPiezas CHECK (cantidad_piezas > 0),

    CONSTRAINT CHK_Estatus CHECK (
        estatus IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')
    ),

    CONSTRAINT UQ_Pedido_NaturalKey UNIQUE (articulo_id, tienda_id, fecha_distribucion)
);
GO

-- ============================================================
-- TABLA: Usuario_Roles
-- ============================================================
CREATE TABLE Usuario_Roles (
    usuario_id            BIGINT       NOT NULL,
    rol_id                BIGINT       NOT NULL,
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_Usuario_Roles PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT FK_UsuarioRoles_Usuario FOREIGN KEY (usuario_id)
        REFERENCES Usuarios(id)
        ON DELETE CASCADE,
    CONSTRAINT FK_UsuarioRoles_Rol FOREIGN KEY (rol_id)
        REFERENCES Roles(id)
        ON DELETE CASCADE
);
GO

-- ============================================================
-- TABLA: Refresh_Tokens
-- ============================================================
CREATE TABLE Refresh_Tokens (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id            BIGINT       NOT NULL,
    token_hash            VARCHAR(255) NOT NULL UNIQUE,
    fecha_expiracion      DATETIME2    NOT NULL,
    revocado              BIT          NOT NULL DEFAULT 0,
    usuario_creacion      VARCHAR(100) NOT NULL,
    usuario_modificacion  VARCHAR(100) NULL,
    created_at            DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_RefreshTokens_Usuario FOREIGN KEY (usuario_id)
        REFERENCES Usuarios(id)
        ON DELETE CASCADE
);
GO

-- ============================================================
-- Indices recomendados para consultas frecuentes
-- ============================================================
CREATE INDEX IX_Articulos_Familia_Activo ON Articulos (familia, activo);
CREATE INDEX IX_Tiendas_Nombre_Activa ON Tiendas (nombre, activa);
CREATE INDEX IX_Pedidos_Fecha_Estatus ON PedidosDistribucion (fecha_distribucion, estatus);
CREATE INDEX IX_Pedidos_Articulo ON PedidosDistribucion (articulo_id);
CREATE INDEX IX_Pedidos_Tienda ON PedidosDistribucion (tienda_id);
CREATE INDEX IX_Usuarios_Activo ON Usuarios (activo);
CREATE INDEX IX_RefreshTokens_Usuario_Revocado ON Refresh_Tokens (usuario_id, revocado);
GO

-- ============================================================
-- STORED PROCEDURE: sp_CrearPedidoDistribucion
-- Implementacion de idempotencia sin idempotency_key.
-- 1) Revisa si existe por clave de negocio
-- 2) Inserta si no existe
-- 3) Ante carrera concurrente (2601/2627), recupera existente
-- ============================================================
CREATE OR ALTER PROCEDURE sp_CrearPedidoDistribucion
    @articuloId        BIGINT,
    @tiendaId          BIGINT,
    @fechaDistribucion DATE,
    @cantidadPiezas    INT,
    @estatus           VARCHAR(50),
    @usuarioCreacion   VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF @usuarioCreacion IS NULL OR LTRIM(RTRIM(@usuarioCreacion)) = ''
    BEGIN
        RAISERROR('usuarioCreacion es obligatorio.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1 FROM Articulos WHERE id = @articuloId AND activo = 1
    )
    BEGIN
        RAISERROR('El articulo con ID %d no existe o esta inactivo.', 16, 1, @articuloId);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1 FROM Tiendas WHERE id = @tiendaId AND activa = 1
    )
    BEGIN
        RAISERROR('La tienda con ID %d no existe o esta inactiva.', 16, 1, @tiendaId);
        RETURN;
    END

    IF @cantidadPiezas <= 0
    BEGIN
        RAISERROR('La cantidad de piezas debe ser mayor a 0.', 16, 1);
        RETURN;
    END

    IF @estatus NOT IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')
    BEGIN
        RAISERROR('Estatus invalido. Valores: PENDIENTE, EN_PROCESO, COMPLETADO, CANCELADO.', 16, 1);
        RETURN;
    END

    -- Primer chequeo para idempotencia por regla de negocio.
    IF EXISTS (
        SELECT 1
        FROM PedidosDistribucion
        WHERE articulo_id = @articuloId
          AND tienda_id = @tiendaId
          AND fecha_distribucion = @fechaDistribucion
    )
    BEGIN
        SELECT
            p.id,
            p.articulo_id,
            p.tienda_id,
            p.fecha_distribucion,
            p.cantidad_piezas,
            p.estatus,
            p.usuario_creacion,
            p.usuario_modificacion,
            p.created_at,
            p.updated_at,
            'EXISTING' AS resultado
        FROM PedidosDistribucion p
        WHERE p.articulo_id = @articuloId
          AND p.tienda_id = @tiendaId
          AND p.fecha_distribucion = @fechaDistribucion;
        RETURN;
    END

    BEGIN TRY
        INSERT INTO PedidosDistribucion (
            articulo_id,
            tienda_id,
            fecha_distribucion,
            cantidad_piezas,
            estatus,
            usuario_creacion,
            usuario_modificacion,
            created_at,
            updated_at
        )
        VALUES (
            @articuloId,
            @tiendaId,
            @fechaDistribucion,
            @cantidadPiezas,
            @estatus,
            @usuarioCreacion,
            NULL,
            GETDATE(),
            GETDATE()
        );

        SELECT
            p.id,
            p.articulo_id,
            p.tienda_id,
            p.fecha_distribucion,
            p.cantidad_piezas,
            p.estatus,
            p.usuario_creacion,
            p.usuario_modificacion,
            p.created_at,
            p.updated_at,
            'CREATED' AS resultado
        FROM PedidosDistribucion p
        WHERE p.id = SCOPE_IDENTITY();
    END TRY
    BEGIN CATCH
        -- 2601/2627: violacion de indice/constraint UNIQUE por concurrencia.
        IF ERROR_NUMBER() IN (2601, 2627)
        BEGIN
            SELECT
                p.id,
                p.articulo_id,
                p.tienda_id,
                p.fecha_distribucion,
                p.cantidad_piezas,
                p.estatus,
                p.usuario_creacion,
                p.usuario_modificacion,
                p.created_at,
                p.updated_at,
                'EXISTING' AS resultado
            FROM PedidosDistribucion p
            WHERE p.articulo_id = @articuloId
              AND p.tienda_id = @tiendaId
              AND p.fecha_distribucion = @fechaDistribucion;
            RETURN;
        END;

        DECLARE @msg  NVARCHAR(2048) = ERROR_MESSAGE();
        DECLARE @sev  INT            = ERROR_SEVERITY();
        DECLARE @stt  INT            = ERROR_STATE();
        RAISERROR(@msg, @sev, @stt);
    END CATCH
END
GO

-- ============================================================
-- STORED PROCEDURE: sp_BuscarPedidosConArticulo
-- ============================================================
CREATE OR ALTER PROCEDURE sp_BuscarPedidosConArticulo
    @sku            VARCHAR(50)  = NULL,
    @familia        VARCHAR(100) = NULL,
    @tiendaCodigo   VARCHAR(50)  = NULL,
    @tiendaNombre   VARCHAR(150) = NULL,
    @estatus        VARCHAR(50)  = NULL,
    @fechaDesde     DATE         = NULL,
    @fechaHasta     DATE         = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        a.id          AS articuloId,
        a.sku,
        a.descripcion AS articuloDescripcion,
        a.familia,
        t.id          AS tiendaId,
        t.codigo,
        t.nombre      AS tiendaNombre,
        p.id          AS pedidoId,
        p.fecha_distribucion,
        p.cantidad_piezas,
        p.estatus,
        p.created_at  AS pedidoFechaCreacion
    FROM Articulos a
    INNER JOIN PedidosDistribucion p ON a.id = p.articulo_id
    INNER JOIN Tiendas t ON t.id = p.tienda_id
    WHERE
        (@sku          IS NULL OR a.sku LIKE '%' + @sku + '%')
        AND (@familia      IS NULL OR a.familia LIKE '%' + @familia + '%')
        AND (@tiendaCodigo IS NULL OR t.codigo LIKE '%' + @tiendaCodigo + '%')
        AND (@tiendaNombre IS NULL OR t.nombre LIKE '%' + @tiendaNombre + '%')
        AND (@estatus      IS NULL OR p.estatus = @estatus)
        AND (@fechaDesde   IS NULL OR p.fecha_distribucion >= @fechaDesde)
        AND (@fechaHasta   IS NULL OR p.fecha_distribucion <= @fechaHasta)
    ORDER BY a.sku, p.fecha_distribucion;
END
GO

-- ============================================================
-- Datos de ejemplo con auditoria
-- ============================================================

INSERT INTO Tiendas (codigo, nombre, ubicacion, activa, usuario_creacion, usuario_modificacion, created_at, updated_at)
VALUES
    ('TND-001', 'Tienda Centro - CDMX', 'Ciudad de Mexico', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('TND-002', 'Tienda Polanco - CDMX', 'Ciudad de Mexico', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('TND-003', 'Tienda Guadalajara Norte', 'Jalisco', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('TND-004', 'Tienda Monterrey Centro', 'Nuevo Leon', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('TND-005', 'Tienda Puebla Angelopolis', 'Puebla', 1, 'seed_admin', NULL, GETDATE(), GETDATE());
GO

INSERT INTO Articulos (sku, descripcion, familia, activo, usuario_creacion, usuario_modificacion, created_at, updated_at)
VALUES
    ('SKU-001', 'Camiseta Basica Blanca Talla M', 'Ropa', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('SKU-002', 'Pantalon Mezclilla Slim Talla 32', 'Ropa', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('SKU-003', 'Zapato Deportivo Negro Talla 27', 'Calzado', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('SKU-004', 'Mochila Escolar Azul 20L', 'Accesorios', 1, 'seed_admin', NULL, GETDATE(), GETDATE());
GO

INSERT INTO Usuarios (username, email, password_hash, activo, usuario_creacion, usuario_modificacion, created_at, updated_at)
VALUES
    ('admin', 'admin@distribucion.local', 'HASH_DEMO_ADMIN', 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('operador1', 'operador1@distribucion.local', 'HASH_DEMO_OPERADOR', 1, 'seed_admin', NULL, GETDATE(), GETDATE());
GO

INSERT INTO Roles (codigo, nombre, usuario_creacion, usuario_modificacion, created_at, updated_at)
VALUES
    ('ADMIN', 'Administrador', 'seed_admin', NULL, GETDATE(), GETDATE()),
    ('OPERADOR', 'Operador', 'seed_admin', NULL, GETDATE(), GETDATE());
GO

INSERT INTO Usuario_Roles (usuario_id, rol_id, usuario_creacion, usuario_modificacion, created_at, updated_at)
VALUES
    (1, 1, 'seed_admin', NULL, GETDATE(), GETDATE()),
    (2, 2, 'seed_admin', NULL, GETDATE(), GETDATE());
GO

INSERT INTO Refresh_Tokens (usuario_id, token_hash, fecha_expiracion, revocado, usuario_creacion, usuario_modificacion, created_at, updated_at)
VALUES
    (1, 'TOKEN_HASH_DEMO_001', DATEADD(DAY, 7, GETDATE()), 0, 'seed_admin', NULL, GETDATE(), GETDATE());
GO

-- Uso de SP con idempotencia por clave natural
EXEC sp_CrearPedidoDistribucion
    @articuloId = 1,
    @tiendaId = 1,
    @fechaDistribucion = '2026-06-05',
    @cantidadPiezas = 80,
    @estatus = 'PENDIENTE',
    @usuarioCreacion = 'operador_api';
GO

-- Si se ejecuta de nuevo con la misma clave (articulo, tienda, fecha), regresa EXISTING.
EXEC sp_CrearPedidoDistribucion
    @articuloId = 1,
    @tiendaId = 1,
    @fechaDistribucion = '2026-06-05',
    @cantidadPiezas = 80,
    @estatus = 'PENDIENTE',
    @usuarioCreacion = 'operador_api';
GO

-- ============================================================
-- Ejemplos de UPDATE con auditoria
-- Regla sugerida: siempre setear usuario_modificacion y updated_at.
-- ============================================================

UPDATE Articulos
SET
    descripcion = 'Camiseta Basica Blanca Talla M - Version 2',
    usuario_modificacion = 'operador_catalogo',
    updated_at = GETDATE()
WHERE sku = 'SKU-001';
GO

UPDATE PedidosDistribucion
SET
    estatus = 'EN_PROCESO',
    usuario_modificacion = 'operador_logistica',
    updated_at = GETDATE()
WHERE articulo_id = 1
  AND tienda_id = 1
  AND fecha_distribucion = '2026-06-05';
GO

-- ============================================================
-- Explicacion breve (auditoria e idempotencia)
-- 1) Auditoria:
--    - usuario_creacion obligatorio en INSERT.
--    - usuario_modificacion se completa en UPDATE.
--    - created_at/updated_at mantienen trazabilidad temporal.
-- 2) Idempotencia:
--    - No se usa idempotency_key.
--    - Se usa UNIQUE(articulo_id, tienda_id, fecha_distribucion) como llave natural.
--    - El SP hace pre-chequeo y en concurrencia maneja 2601/2627 para devolver EXISTING.
-- ============================================================
