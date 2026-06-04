package com.pruebatecnica.distribucion.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.distribucion.dto.pedido.PedidoBusquedaResponse;
import com.pruebatecnica.distribucion.dto.pedido.PedidoCreateRequest;
import com.pruebatecnica.distribucion.dto.pedido.PedidoResponse;
import com.pruebatecnica.distribucion.dto.pedido.PedidoUpdateRequest;
import com.pruebatecnica.distribucion.service.PedidoDistribucionService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api")
public class PedidoDistribucionController {

    private final PedidoDistribucionService pedidoService;

    public PedidoDistribucionController(PedidoDistribucionService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoBusquedaResponse>> buscar(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String familia,
            @RequestParam(required = false) String tiendaCodigo,
            @RequestParam(required = false) String tiendaNombre,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        return ResponseEntity.ok(
                pedidoService.buscar(sku, familia, tiendaCodigo, tiendaNombre, estatus, fechaDesde, fechaHasta));
    }

    @PostMapping("/articulos/{id}/pedidos")
    public ResponseEntity<PedidoResponse> create(@PathVariable Long id,
            @Valid @RequestBody PedidoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.create(id, request));
    }

    @PutMapping("/pedidos/{id}")
    public ResponseEntity<PedidoResponse> update(@PathVariable Long id,
            @Valid @RequestBody PedidoUpdateRequest request) {
        return ResponseEntity.ok(pedidoService.update(id, request));
    }

    @DeleteMapping("/pedidos/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pedidoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
