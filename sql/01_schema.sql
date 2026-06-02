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
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Tiendas' AND xtype='U')
BEGIN
    CREATE TABLE Tiendas (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        codigo      VARCHAR(50)  NOT NULL UNIQUE,
        nombre      VARCHAR(150) NOT NULL,
        ubicacion   VARCHAR(255) NULL,
        activa      BIT          NOT NULL DEFAULT 1,
        created_at  DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at  DATETIME2    NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='PedidosDistribucion' AND xtype='U')
BEGIN
    CREATE TABLE PedidosDistribucion (
        id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
        articulo_id         BIGINT       NOT NULL,
        tienda_id           BIGINT       NOT NULL,
        fecha_distribucion  DATE         NOT NULL,
        cantidad_piezas     INT          NOT NULL CHECK (cantidad_piezas > 0),
        estatus             VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE',
        idempotency_key     VARCHAR(100) NULL UNIQUE,
        created_at          DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at          DATETIME2    NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_Pedido_Articulo FOREIGN KEY (articulo_id)
            REFERENCES Articulos(id)
            ON DELETE CASCADE,

        CONSTRAINT FK_Pedido_Tienda FOREIGN KEY (tienda_id)
            REFERENCES Tiendas(id)
            ON DELETE NO ACTION,

        CONSTRAINT CHK_Estatus CHECK (
            estatus IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')
        )
    );
END
GO

-- ============================================================
-- TABLAS: Autenticación
-- ============================================================
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Usuarios' AND xtype='U')
BEGIN
    CREATE TABLE Usuarios (
        id             BIGINT IDENTITY(1,1) PRIMARY KEY,
        username       VARCHAR(80)  NOT NULL UNIQUE,
        email          VARCHAR(150) NOT NULL UNIQUE,
        password_hash  VARCHAR(255) NOT NULL,
        activo         BIT          NOT NULL DEFAULT 1,
        created_at     DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at     DATETIME2    NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Roles' AND xtype='U')
BEGIN
    CREATE TABLE Roles (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        codigo       VARCHAR(50)  NOT NULL UNIQUE,
        nombre       VARCHAR(100) NOT NULL,
        created_at   DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at   DATETIME2    NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Usuario_Roles' AND xtype='U')
BEGIN
    CREATE TABLE Usuario_Roles (
        usuario_id   BIGINT    NOT NULL,
        rol_id       BIGINT    NOT NULL,
        created_at   DATETIME2 NOT NULL DEFAULT GETDATE(),

        CONSTRAINT PK_Usuario_Roles PRIMARY KEY (usuario_id, rol_id),
        CONSTRAINT FK_UsuarioRoles_Usuario FOREIGN KEY (usuario_id)
            REFERENCES Usuarios(id)
            ON DELETE CASCADE,
        CONSTRAINT FK_UsuarioRoles_Rol FOREIGN KEY (rol_id)
            REFERENCES Roles(id)
            ON DELETE CASCADE
    );
END
GO

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Refresh_Tokens' AND xtype='U')
BEGIN
    CREATE TABLE Refresh_Tokens (
        id             BIGINT IDENTITY(1,1) PRIMARY KEY,
        usuario_id     BIGINT       NOT NULL,
        token_hash     VARCHAR(255) NOT NULL UNIQUE,
        fecha_expiracion DATETIME2  NOT NULL,
        revocado       BIT          NOT NULL DEFAULT 0,
        created_at     DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at     DATETIME2    NOT NULL DEFAULT GETDATE(),

        CONSTRAINT FK_RefreshTokens_Usuario FOREIGN KEY (usuario_id)
            REFERENCES Usuarios(id)
            ON DELETE CASCADE
    );
END
GO

-- ============================================================
-- STORED PROCEDURE: sp_CrearPedidoDistribucion
-- ============================================================
CREATE OR ALTER PROCEDURE sp_CrearPedidoDistribucion
    @articuloId        BIGINT,
    @tiendaId          BIGINT,
    @fechaDistribucion DATE,
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
                p.tienda_id,
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

    IF NOT EXISTS (
        SELECT 1 FROM Tiendas
        WHERE id = @tiendaId AND activa = 1
    )
    BEGIN
        RAISERROR('La tienda con ID %d no existe o está inactiva.', 16, 1, @tiendaId);
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
        articulo_id, tienda_id, fecha_distribucion,
        cantidad_piezas, estatus, idempotency_key
    )
    VALUES (
        @articuloId, @tiendaId, @fechaDistribucion,
        @cantidadPiezas, @estatus, @idempotencyKey
    );

    SELECT
        p.id,
        p.articulo_id,
        p.tienda_id,
        p.fecha_distribucion,
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
    @tiendaCodigo   VARCHAR(50)  = NULL,
    @tiendaNombre   VARCHAR(150) = NULL,
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
        t.id           AS tiendaId,
        t.codigo,
        t.nombre       AS tiendaNombre,
        p.id           AS pedidoId,
        p.fecha_distribucion,
        p.cantidad_piezas,
        p.estatus,
        p.created_at   AS pedidoFechaCreacion
    FROM Articulos a
    INNER JOIN PedidosDistribucion p ON a.id = p.articulo_id
    INNER JOIN Tiendas t ON t.id = p.tienda_id
    WHERE
        (@sku           IS NULL OR a.sku            LIKE '%' + @sku + '%')
        AND (@familia       IS NULL OR a.familia        LIKE '%' + @familia + '%')
        AND (@tiendaCodigo  IS NULL OR t.codigo         LIKE '%' + @tiendaCodigo + '%')
        AND (@tiendaNombre  IS NULL OR t.nombre         LIKE '%' + @tiendaNombre + '%')
        AND (@estatus       IS NULL OR p.estatus        = @estatus)
        AND (@fechaDesde    IS NULL OR p.fecha_distribucion >= @fechaDesde)
        AND (@fechaHasta    IS NULL OR p.fecha_distribucion <= @fechaHasta)
    ORDER BY a.sku, p.fecha_distribucion;
END
GO

-- ============================================================
-- Datos de Tiendas
-- ============================================================
INSERT INTO Tiendas (codigo, nombre, ubicacion) VALUES
    ('TND-001', 'Tienda Centro - CDMX', 'Ciudad de México'),
    ('TND-002', 'Tienda Polanco - CDMX', 'Ciudad de México'),
    ('TND-003', 'Tienda Guadalajara Norte', 'Jalisco'),
    ('TND-004', 'Tienda Monterrey Centro', 'Nuevo León'),
    ('TND-005', 'Tienda Puebla Angelópolis', 'Puebla');
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

INSERT INTO PedidosDistribucion (articulo_id, fecha_distribucion, tienda_id, cantidad_piezas, estatus) VALUES
    (1, '2025-07-10', 1, 50, 'PENDIENTE'),
    (1, '2025-07-15', 2, 30, 'EN_PROCESO'),
    (2, '2025-07-12', 3, 25, 'PENDIENTE'),
    (3, '2025-07-20', 4, 40, 'COMPLETADO'),
    (4, '2025-07-18', 5, 60, 'PENDIENTE');
GO
