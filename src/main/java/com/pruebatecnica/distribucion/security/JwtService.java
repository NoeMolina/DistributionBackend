package com.pruebatecnica.distribucion.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.pruebatecnica.distribucion.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateAccessToken(Usuario usuario, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(usuario.getUsername())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claims(Map.of(
                "userId", usuario.getId(),
                "email", usuario.getEmail(),
                "roles", roles,
                "tokenType", "access"
            ))
            .signWith(signingKey())
            .compact();
    }

    public String generateRefreshToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getRefreshTokenTtl());

        return Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(usuario.getUsername())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .id(UUID.randomUUID().toString())
            .claims(Map.of(
                "userId", usuario.getId(),
                "tokenType", "refresh"
            ))
            .signWith(signingKey())
            .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> roleList) {
            return roleList.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public String extractTokenType(String token) {
        Object tokenType = parseClaims(token).get("tokenType");
        return tokenType == null ? null : String.valueOf(tokenType);
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    public long getRefreshTokenTtlSeconds() {
        return properties.getRefreshTokenTtl().toSeconds();
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash del token", ex);
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
