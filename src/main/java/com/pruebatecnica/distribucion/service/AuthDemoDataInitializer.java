package com.pruebatecnica.distribucion.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pruebatecnica.distribucion.entity.Rol;
import com.pruebatecnica.distribucion.entity.Usuario;
import com.pruebatecnica.distribucion.entity.UsuarioRol;
import com.pruebatecnica.distribucion.repository.RolRepository;
import com.pruebatecnica.distribucion.repository.UsuarioRepository;
import com.pruebatecnica.distribucion.repository.UsuarioRolRepository;

@Component
public class AuthDemoDataInitializer implements CommandLineRunner {

    private static final String SYSTEM_USER = "system";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDemoDataInitializer(
        UsuarioRepository usuarioRepository,
        RolRepository rolRepository,
        UsuarioRolRepository usuarioRolRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Rol adminRole = ensureRole("ADMIN", "Administrador");
        Rol operatorRole = ensureRole("OPERADOR", "Operador");

        Usuario admin = ensureUser("admin", "admin@distribucion.local", "Admin123*");
        Usuario operador = ensureUser("operador1", "operador1@distribucion.local", "Operador123*");

        ensureUserRole(admin, adminRole);
        ensureUserRole(operador, operatorRole);
    }

    private Rol ensureRole(String codigo, String nombre) {
        return rolRepository.findByCodigo(codigo)
            .map(role -> {
                if (!nombre.equals(role.getNombre())) {
                    role.setNombre(nombre);
                    role.setUsuarioModificacion(SYSTEM_USER);
                    rolRepository.save(role);
                }
                return role;
            })
            .orElseGet(() -> {
                Rol role = new Rol();
                role.setCodigo(codigo);
                role.setNombre(nombre);
                role.setUsuarioCreacion(SYSTEM_USER);
                role.setUsuarioModificacion(null);
                return rolRepository.save(role);
            });
    }

    private Usuario ensureUser(String username, String email, String rawPassword) {
        return usuarioRepository.findByUsername(username)
            .map(user -> {
                if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    user.setUsuarioModificacion(SYSTEM_USER);
                    usuarioRepository.save(user);
                }
                return user;
            })
            .orElseGet(() -> {
                Usuario user = new Usuario();
                user.setUsername(username);
                user.setEmail(email);
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setActivo(Boolean.TRUE);
                user.setUsuarioCreacion(SYSTEM_USER);
                user.setUsuarioModificacion(null);
                return usuarioRepository.save(user);
            });
    }

    private void ensureUserRole(Usuario usuario, Rol rol) {
        UsuarioRol.UsuarioRolId id = new UsuarioRol.UsuarioRolId();
        id.setUsuarioId(usuario.getId());
        id.setRolId(rol.getId());

        if (usuarioRolRepository.existsById(id)) {
            return;
        }

        UsuarioRol userRole = new UsuarioRol();
        userRole.setId(id);
        userRole.setUsuario(usuario);
        userRole.setRol(rol);
        userRole.setUsuarioCreacion(SYSTEM_USER);
        userRole.setUsuarioModificacion(null);
        usuarioRolRepository.save(userRole);
    }
}