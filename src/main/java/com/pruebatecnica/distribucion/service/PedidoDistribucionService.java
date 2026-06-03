package com.pruebatecnica.distribucion.service;

import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        Articulo articulo = articuloRepository.findById(articuloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));

        Tienda tienda = tiendaRepository.findById(request.tiendaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tienda no encontrada"));

        validateArticuloActivo(articulo);
        validateTiendaActiva(tienda);
        validateNoDuplicate(articuloId, request.tiendaId(), request.fechaDistribucion());

        PedidoDistribucion pedido = new PedidoDistribucion();
        pedido.setArticulo(articulo);
        pedido.setTienda(tienda);
        pedido.setFechaDistribucion(request.fechaDistribucion());
        pedido.setCantidadPiezas(request.cantidadPiezas());
        pedido.setEstatus(request.estatus());
        pedido.setUsuarioCreacion(currentUsername());
        pedido.setUsuarioModificacion(null);

        try {
            return toResponse(pedidoRepository.save(pedido));
        } catch (DataIntegrityViolationException ex) {
            PedidoDistribucion existente = pedidoRepository
                    .findByArticulo_IdAndTienda_IdAndFechaDistribucion(articuloId, request.tiendaId(),
                            request.fechaDistribucion())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo crear el pedido"));
            return toResponse(existente);
        }
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
        pedidoRepository.delete(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponse findById(Long id) {
        PedidoDistribucion pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        return toResponse(pedido);
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

    private void validateNoDuplicate(Long articuloId, Long tiendaId, LocalDate fechaDistribucion) {
        pedidoRepository.findByArticulo_IdAndTienda_IdAndFechaDistribucion(articuloId, tiendaId, fechaDistribucion)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un pedido con esa combinacion de negocio");
                });
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
