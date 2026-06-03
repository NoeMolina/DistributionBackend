package com.pruebatecnica.distribucion.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.pruebatecnica.distribucion.dto.articulo.ArticuloCreateRequest;
import com.pruebatecnica.distribucion.dto.articulo.ArticuloDetailResponse;
import com.pruebatecnica.distribucion.dto.articulo.ArticuloSummaryResponse;
import com.pruebatecnica.distribucion.dto.articulo.ArticuloUpdateRequest;
import com.pruebatecnica.distribucion.dto.articulo.PedidoDistribucionResumenResponse;
import com.pruebatecnica.distribucion.entity.Articulo;
import com.pruebatecnica.distribucion.entity.PedidoDistribucion;
import com.pruebatecnica.distribucion.repository.ArticuloRepository;
import com.pruebatecnica.distribucion.repository.PedidoDistribucionRepository;

@Service
@Transactional
public class ArticuloService {

    private static final String SYSTEM_USER = "system";

    private final ArticuloRepository articuloRepository;
    private final PedidoDistribucionRepository pedidoDistribucionRepository;

    public ArticuloService(
            ArticuloRepository articuloRepository,
            PedidoDistribucionRepository pedidoDistribucionRepository) {
        this.articuloRepository = articuloRepository;
        this.pedidoDistribucionRepository = pedidoDistribucionRepository;
    }

    @Transactional(readOnly = true)
    public List<ArticuloSummaryResponse> findAll() {
        return articuloRepository.findAll()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticuloDetailResponse findById(Long id) {
        Articulo articulo = articuloRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));

        List<PedidoDistribucionResumenResponse> pedidos = pedidoDistribucionRepository
                .findByArticuloId(id)
                .stream()
                .map(this::toPedidoResumenResponse)
                .toList();

        return toDetailResponse(articulo, pedidos);
    }

    public ArticuloSummaryResponse create(ArticuloCreateRequest request) {
        validateSku(request.sku());
        if (articuloRepository.findBySku(request.sku()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un articulo con ese SKU");
        }

        Articulo articulo = new Articulo();
        applyRequest(articulo, request.sku(), request.descripcion(), request.familia(), request.activo());
        articulo.setUsuarioCreacion(SYSTEM_USER);
        articulo.setUsuarioModificacion(null);

        try {
            return toSummaryResponse(articuloRepository.save(articulo));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pudo crear el articulo. Verifica que el SKU sea unico.");
        }
    }

    public ArticuloSummaryResponse update(Long id, ArticuloUpdateRequest request) {
        validateSku(request.sku());
        Articulo articulo = articuloRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));

        articuloRepository.findBySku(request.sku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro articulo con ese SKU");
                });

        applyRequest(articulo, request.sku(), request.descripcion(), request.familia(), request.activo());
        articulo.setUsuarioModificacion(SYSTEM_USER);

        try {
            return toSummaryResponse(articuloRepository.save(articulo));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pudo actualizar el articulo. Verifica el SKU.");
        }
    }

    public void delete(Long id) {
        Articulo articulo = articuloRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articulo no encontrado"));

        if (!articulo.getActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El articulo ya fue eliminado");
        }

        List<PedidoDistribucion> pedidos = pedidoDistribucionRepository.findByArticuloId(id);
        pedidos.forEach(pedido -> {
            if ("PENDIENTE".equals(pedido.getEstatus())) {
                pedido.setEstatus("CANCELADO");
                pedido.setUsuarioModificacion(SYSTEM_USER);
            }
        });
        pedidoDistribucionRepository.saveAll(pedidos);

        // Soft delete del artículo
        articulo.setActivo(false);
        articulo.setUsuarioModificacion(SYSTEM_USER);
        articuloRepository.save(articulo);
    }

    private void applyRequest(Articulo articulo, String sku, String descripcion, String familia, Boolean activo) {
        articulo.setSku(sku.trim());
        articulo.setDescripcion(descripcion.trim());
        articulo.setFamilia(familia.trim());
        articulo.setActivo(activo == null ? Boolean.TRUE : activo);
    }

    private void validateSku(String sku) {
        if (!StringUtils.hasText(sku)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sku es obligatorio");
        }
    }

    private ArticuloSummaryResponse toSummaryResponse(Articulo articulo) {
        return new ArticuloSummaryResponse(
                articulo.getId(),
                articulo.getSku(),
                articulo.getDescripcion(),
                articulo.getFamilia(),
                articulo.getActivo(),
                articulo.getCreatedAt(),
                articulo.getUpdatedAt());
    }

    private ArticuloDetailResponse toDetailResponse(Articulo articulo,
            List<PedidoDistribucionResumenResponse> pedidos) {
        return new ArticuloDetailResponse(
                articulo.getId(),
                articulo.getSku(),
                articulo.getDescripcion(),
                articulo.getFamilia(),
                articulo.getActivo(),
                articulo.getCreatedAt(),
                articulo.getUpdatedAt(),
                pedidos);
    }

    private PedidoDistribucionResumenResponse toPedidoResumenResponse(PedidoDistribucion pedido) {
        return new PedidoDistribucionResumenResponse(
                pedido.getId(),
                pedido.getTienda().getId(),
                pedido.getTienda().getCodigo(),
                pedido.getTienda().getNombre(),
                pedido.getFechaDistribucion(),
                pedido.getCantidadPiezas(),
                pedido.getEstatus(),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt());
    }
}
