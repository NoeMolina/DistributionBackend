package com.pruebatecnica.distribucion.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pruebatecnica.distribucion.entity.PedidoDistribucion;

public interface PedidoDistribucionRepository extends JpaRepository<PedidoDistribucion, Long> {
        Optional<PedidoDistribucion> findByArticulo_IdAndTienda_IdAndFechaDistribucion(Long articuloId, Long tiendaId,
                        LocalDate fechaDistribucion);

        @EntityGraph(attributePaths = { "tienda" })
        List<PedidoDistribucion> findByArticuloIdAndActivoTrue(Long articuloId);

        List<PedidoDistribucion> findByEstatus(String estatus);

        @Query(value = """
                        EXEC sp_CrearPedidoDistribucion
                            @articuloId        = :articuloId,
                            @tiendaId          = :tiendaId,
                            @fechaDistribucion = :fechaDistribucion,
                            @cantidadPiezas    = :cantidadPiezas,
                            @estatus           = :estatus,
                            @usuarioCreacion   = :usuarioCreacion
                        """, nativeQuery = true)
        List<Object[]> crearConSP(
                        @Param("articuloId") Long articuloId,
                        @Param("tiendaId") Long tiendaId,
                        @Param("fechaDistribucion") LocalDate fechaDistribucion,
                        @Param("cantidadPiezas") Integer cantidadPiezas,
                        @Param("estatus") String estatus,
                        @Param("usuarioCreacion") String usuarioCreacion);

        @Query(value = """
                        EXEC sp_BuscarPedidosConArticulo
                            @sku          = :sku,
                            @familia      = :familia,
                            @tiendaCodigo = :tiendaCodigo,
                            @tiendaNombre = :tiendaNombre,
                            @estatus      = :estatus,
                            @fechaDesde   = :fechaDesde,
                            @fechaHasta   = :fechaHasta
                        """, nativeQuery = true)
        List<Object[]> buscarConFiltros(
                        @Param("sku") String sku,
                        @Param("familia") String familia,
                        @Param("tiendaCodigo") String tiendaCodigo,
                        @Param("tiendaNombre") String tiendaNombre,
                        @Param("estatus") String estatus,
                        @Param("fechaDesde") LocalDate fechaDesde,
                        @Param("fechaHasta") LocalDate fechaHasta);
}
