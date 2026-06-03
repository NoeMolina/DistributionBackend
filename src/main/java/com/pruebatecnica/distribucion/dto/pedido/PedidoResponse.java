package com.pruebatecnica.distribucion.dto.pedido;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoResponse(
    Long id,
    Long articuloId,
    String articuloSku,
    Long tiendaId,
    String tiendaCodigo,
    String tiendaNombre,
    LocalDate fechaDistribucion,
    Integer cantidadPiezas,
    String estatus,
    String usuarioCreacion,
    String usuarioModificacion,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
