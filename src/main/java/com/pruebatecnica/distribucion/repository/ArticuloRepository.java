package com.pruebatecnica.distribucion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.distribucion.entity.Articulo;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
    Optional<Articulo> findBySku(String sku);
}
