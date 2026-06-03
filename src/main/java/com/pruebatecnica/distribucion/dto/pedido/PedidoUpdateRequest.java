package com.pruebatecnica.distribucion.dto.pedido;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PedidoUpdateRequest(
    @NotNull(message = "tiendaId es obligatoria")
    Long tiendaId,

    @NotNull(message = "fechaDistribucion es obligatoria")
    @FutureOrPresent(message = "fechaDistribucion debe ser hoy o una fecha futura")
    LocalDate fechaDistribucion,

    @NotNull(message = "cantidadPiezas es obligatoria")
    @Min(value = 1, message = "cantidadPiezas debe ser mayor a 0")
    Integer cantidadPiezas,

    @NotNull(message = "estatus es obligatorio")
    @Pattern(regexp = "PENDIENTE|EN_PROCESO|COMPLETADO|CANCELADO", message = "estatus invalido")
    String estatus
) {}
