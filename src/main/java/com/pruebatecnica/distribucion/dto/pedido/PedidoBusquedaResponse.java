package com.pruebatecnica.distribucion.dto.pedido;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoBusquedaResponse(
    Long articuloId,
    String sku,
    String articuloDescripcion,
    String familia,
    Long tiendaId,
    String tiendaCodigo,
    String tiendaNombre,
    Long pedidoId,
    LocalDate fechaDistribucion,
    Integer cantidadPiezas,
    String estatus,
    LocalDateTime pedidoFechaCreacion
) {}