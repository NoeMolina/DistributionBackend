package com.pruebatecnica.distribucion.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pruebatecnica.distribucion.entity.PedidoDistribucion;

public interface PedidoDistribucionRepository extends JpaRepository<PedidoDistribucion, Long> {
    Optional<PedidoDistribucion> findByArticulo_IdAndTienda_IdAndFechaDistribucion(Long articuloId, Long tiendaId, LocalDate fechaDistribucion);
    @EntityGraph(attributePaths = {"tienda"})
    List<PedidoDistribucion> findByArticuloId(Long articuloId);
    List<PedidoDistribucion> findByEstatus(String estatus);
}
