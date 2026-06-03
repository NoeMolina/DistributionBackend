package com.pruebatecnica.distribucion.dto.articulo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticuloUpdateRequest(
    @NotBlank(message = "sku es obligatorio")
    @Size(max = 50, message = "sku no puede exceder 50 caracteres")
    String sku,

    @NotBlank(message = "descripcion es obligatoria")
    @Size(max = 255, message = "descripcion no puede exceder 255 caracteres")
    String descripcion,

    @NotBlank(message = "familia es obligatoria")
    @Size(max = 100, message = "familia no puede exceder 100 caracteres")
    String familia,

    Boolean activo
) {}
