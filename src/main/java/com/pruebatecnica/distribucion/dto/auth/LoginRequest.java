package com.pruebatecnica.distribucion.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "login es obligatorio")
    String login,
    @NotBlank(message = "password es obligatorio")
    String password
) {}
