package com.pruebatecnica.distribucion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.distribucion.entity.Tienda;

public interface TiendaRepository extends JpaRepository<Tienda, Long> {
    Optional<Tienda> findByCodigo(String codigo);
}
