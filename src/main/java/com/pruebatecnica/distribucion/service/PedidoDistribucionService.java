package com.pruebatecnica.distribucion.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pruebatecnica.distribucion.dto.pedido.PedidoBusquedaResponse;
import com.pruebatecnica.distribucion.dto.pedido.PedidoCreateRequest;
import com.pruebatecnica.distribucion.dto.pedido.PedidoResponse;
import com.pruebatecnica.distribucion.dto.pedido.PedidoUpdateRequest;
import com.pruebatecnica.distribucion.entity.Articulo;
import com.pruebatecnica.distribucion.entity.PedidoDistribucion;
import com.pruebatecnica.distribucion.entity.Tienda;
import com.pruebatecnica.distribucion.repository.ArticuloRepository;
import com.pruebatecnica.distribucion.repository.PedidoDistribucionRepository;
import com.pruebatecnica.distribucion.repository.TiendaRepository;

@Service
@Transactional
public class PedidoDistribucionService {

    private static final String SYSTEM_USER = "system";

    private final PedidoDistribucionRepository pedidoRepository;
    private final ArticuloRepository articuloRepository;
    private final TiendaRepository tiendaRepository;

    public PedidoDistribucionService(
            PedidoDistribucionRepository pedidoRepository,
            ArticuloRepository articuloRepository,
            TiendaRepository tiendaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.articuloRepository = articuloRepository;
        this.tiendaRepository = tiendaRepository;
    }

    public PedidoResponse create(Long articuloId, PedidoCreateRequest request) {
        List<Object[]> rows = pedidoRepository.crearConSP(
                articuloId,
                request.tiendaId(),
                request.fechaDistribucion(),
                request.cantidadPiezas(),
                request.estatus(),
                currentUsername());

        if (rows == null || rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el pedido");
        }

        Object[] row = rows.get(0);

        return new PedidoResponse(
                ((Number) row[0]).longValue(), // id
                articuloId, // articuloId
                null, // articuloSku
                ((Number) row[2]).longValue(), // tiendaId
                null, // tiendaCodigo
                null, // tiendaNombre
                ((java.sql.Date) row[3]).toLocalDate(), // fechaDistribucion
                ((Number) row[4]).intValue(), // cantidadPiezas
                (String) row[5], // estatus
                (String) row[6], // usuarioCreacion
                (String) row[7], // usuarioModificacion
                ((java.sql.Timestamp) row[8]).toLocalDateTime(), // createdAt
                ((java.sql.Timestamp) row[9]).toLocalDateTime() // updatedAt
        );
    }

    public PedidoResponse update(Long id, PedidoUpdateRequest request) {
        PedidoDistribucion pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        Articulo articulo = pedido.getArticulo();
        Tienda tienda = tiendaRepository.findById(request.tiendaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tienda no encontrada"));

        validateArticuloActivo(articulo);
        validateTiendaActiva(tienda);

        pedidoRepository
                .findByArticulo_IdAndTienda_IdAndFechaDistribucion(articulo.getId(), request.tiendaId(),
                        request.fechaDistribucion())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Ya existe un pedido con esa combinacion de negocio");
                });

        pedido.setTienda(tienda);
        pedido.setFechaDistribucion(request.fechaDistribucion());
        pedido.setCantidadPiezas(request.cantidadPiezas());
        pedido.setEstatus(request.estatus());
        pedido.setUsuarioModificacion(currentUsername());

        try {
            return toResponse(pedidoRepository.save(pedido));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo actualizar el pedido");
        }
    }

    public void delete(Long id) {
        PedidoDistribucion pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        if (!pedido.getActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya fue eliminado");
        }

        pedido.setActivo(false);
        pedido.setEstatus("CANCELADO");
        pedido.setUsuarioModificacion(SYSTEM_USER);
        pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponse findById(Long id) {
        PedidoDistribucion pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoBusquedaResponse> buscar(
            String sku, String familia, String tiendaCodigo,
            String tiendaNombre, String estatus,
            LocalDate fechaDesde, LocalDate fechaHasta) {

        return pedidoRepository
                .buscarConFiltros(sku, familia, tiendaCodigo, tiendaNombre, estatus, fechaDesde, fechaHasta)
                .stream()
                .map(row -> new PedidoBusquedaResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue(),
                        (String) row[5],
                        (String) row[6],
                        ((Number) row[7]).longValue(),
                        ((java.sql.Date) row[8]).toLocalDate(),
                        ((Number) row[9]).intValue(),
                        (String) row[10],
                        ((java.sql.Timestamp) row[11]).toLocalDateTime()))
                .toList();
    }

    private void validateArticuloActivo(Articulo articulo) {
        if (!Boolean.TRUE.equals(articulo.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El articulo esta inactivo");
        }
    }

    private void validateTiendaActiva(Tienda tienda) {
        if (!Boolean.TRUE.equals(tienda.getActiva())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La tienda esta inactiva");
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return authentication.getName();
        }
        return SYSTEM_USER;
    }

    private PedidoResponse toResponse(PedidoDistribucion pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getArticulo().getId(),
                pedido.getArticulo().getSku(),
                pedido.getTienda().getId(),
                pedido.getTienda().getCodigo(),
                pedido.getTienda().getNombre(),
                pedido.getFechaDistribucion(),
                pedido.getCantidadPiezas(),
                pedido.getEstatus(),
                pedido.getUsuarioCreacion(),
                pedido.getUsuarioModificacion(),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt());
    }
}
