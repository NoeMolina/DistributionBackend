package com.pruebatecnica.distribucion.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.distribucion.entity.PedidoDistribucion;

public interface PedidoDistribucionRepository extends JpaRepository<PedidoDistribucion, Long> {
    Optional<PedidoDistribucion> findByIdempotencyKey(String idempotencyKey);
    List<PedidoDistribucion> findByEstatus(String estatus);
}
