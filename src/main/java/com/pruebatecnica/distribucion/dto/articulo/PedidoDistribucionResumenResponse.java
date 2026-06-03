package com.pruebatecnica.distribucion.dto.articulo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoDistribucionResumenResponse(
    Long id,
    Long tiendaId,
    String tiendaCodigo,
    String tiendaNombre,
    LocalDate fechaDistribucion,
    Integer cantidadPiezas,
    String estatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
