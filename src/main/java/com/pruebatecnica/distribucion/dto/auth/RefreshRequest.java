package com.pruebatecnica.distribucion.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "refreshToken es obligatorio")
    String refreshToken
) {}
