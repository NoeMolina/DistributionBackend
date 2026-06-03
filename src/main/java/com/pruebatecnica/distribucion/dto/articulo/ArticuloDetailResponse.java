package com.pruebatecnica.distribucion.dto.articulo;

import java.time.LocalDateTime;
import java.util.List;

public record ArticuloDetailResponse(
    Long id,
    String sku,
    String descripcion,
    String familia,
    Boolean activo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<PedidoDistribucionResumenResponse> pedidosDistribucion
) {}
