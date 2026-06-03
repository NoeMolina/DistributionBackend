package com.pruebatecnica.distribucion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.distribucion.entity.UsuarioRol;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRol.UsuarioRolId> {

	List<UsuarioRol> findByUsuario_Id(Long usuarioId);

	List<UsuarioRol> findByRol_Id(Long rolId);

}
