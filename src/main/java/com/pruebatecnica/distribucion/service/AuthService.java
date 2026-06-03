package com.pruebatecnica.distribucion.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pruebatecnica.distribucion.dto.auth.AuthResponse;
import com.pruebatecnica.distribucion.dto.auth.AuthResponse.UserResponse;
import com.pruebatecnica.distribucion.dto.auth.LoginRequest;
import com.pruebatecnica.distribucion.dto.auth.LogoutRequest;
import com.pruebatecnica.distribucion.dto.auth.RefreshRequest;
import com.pruebatecnica.distribucion.entity.RefreshToken;
import com.pruebatecnica.distribucion.entity.Usuario;
import com.pruebatecnica.distribucion.entity.UsuarioRol;
import com.pruebatecnica.distribucion.exception.AuthException;
import com.pruebatecnica.distribucion.repository.RefreshTokenRepository;
import com.pruebatecnica.distribucion.repository.UsuarioRepository;
import com.pruebatecnica.distribucion.repository.UsuarioRolRepository;
import com.pruebatecnica.distribucion.security.JwtService;

@Service
@Transactional
public class AuthService {

    private static final String SYSTEM_USER = "system";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UsuarioRepository usuarioRepository,
        UsuarioRolRepository usuarioRolRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = findUser(request.login());
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AuthException("Usuario inactivo");
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new AuthException("Credenciales invalidas");
        }

        List<String> roles = loadRoles(usuario.getId());
        String accessToken = jwtService.generateAccessToken(usuario, roles);
        String refreshToken = jwtService.generateRefreshToken(usuario);
        saveRefreshToken(usuario, refreshToken);

        return buildResponse(usuario, roles, accessToken, refreshToken);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new AuthException("refreshToken es obligatorio");
        }

        RefreshToken storedToken = findValidRefreshToken(request.refreshToken());
        if (Boolean.TRUE.equals(storedToken.getRevocado())) {
            throw new AuthException("El refresh token fue revocado");
        }
        if (storedToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new AuthException("El refresh token expiro");
        }
        if (!jwtService.isTokenValid(request.refreshToken()) || !"refresh".equals(jwtService.extractTokenType(request.refreshToken()))) {
            throw new AuthException("Refresh token invalido");
        }

        Usuario usuario = storedToken.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AuthException("Usuario inactivo");
        }

        List<String> roles = loadRoles(usuario.getId());
        String accessToken = jwtService.generateAccessToken(usuario, roles);
        return buildResponse(usuario, roles, accessToken, request.refreshToken());
    }

    public void logout(LogoutRequest request) {
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new AuthException("refreshToken es obligatorio");
        }

        RefreshToken storedToken = findValidRefreshToken(request.refreshToken());
        storedToken.setRevocado(Boolean.TRUE);
        storedToken.setUsuarioModificacion(SYSTEM_USER);
        refreshTokenRepository.save(storedToken);
    }

    private Usuario findUser(String login) {
        return usuarioRepository.findByUsername(login)
            .or(() -> usuarioRepository.findByEmail(login))
            .orElseThrow(() -> new AuthException("Credenciales invalidas"));
    }

    private List<String> loadRoles(Long usuarioId) {
        return usuarioRolRepository.findByUsuario_Id(usuarioId)
            .stream()
            .map(UsuarioRol::getRol)
            .map(rol -> rol.getCodigo())
            .distinct()
            .toList();
    }

    private void saveRefreshToken(Usuario usuario, String rawRefreshToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash(jwtService.hashToken(rawRefreshToken));
        refreshToken.setFechaExpiracion(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds()));
        refreshToken.setRevocado(Boolean.FALSE);
        refreshToken.setUsuarioCreacion(usuario.getUsername());
        refreshToken.setUsuarioModificacion(null);
        refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken findValidRefreshToken(String rawRefreshToken) {
        String tokenHash = jwtService.hashToken(rawRefreshToken);
        return refreshTokenRepository.findByTokenHashAndRevocadoFalse(tokenHash)
            .orElseThrow(() -> new AuthException("Refresh token invalido"));
    }

    private AuthResponse buildResponse(Usuario usuario, List<String> roles, String accessToken, String refreshToken) {
        return new AuthResponse(
            "Bearer",
            accessToken,
            refreshToken,
            jwtService.getAccessTokenTtlSeconds(),
            new UserResponse(usuario.getId(), usuario.getUsername(), usuario.getEmail(), roles)
        );
    }
}
