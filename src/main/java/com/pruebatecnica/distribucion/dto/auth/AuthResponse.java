package com.pruebatecnica.distribucion.dto.auth;

import java.util.List;

public record AuthResponse(
    String tokenType,
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    UserResponse user
) {
    public record UserResponse(
        Long id,
        String username,
        String email,
        List<String> roles
    ) {}
}
