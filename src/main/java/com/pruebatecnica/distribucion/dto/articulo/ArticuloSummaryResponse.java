package com.pruebatecnica.distribucion.dto.articulo;

import java.time.LocalDateTime;

public record ArticuloSummaryResponse(
    Long id,
    String sku,
    String descripcion,
    String familia,
    Boolean activo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
