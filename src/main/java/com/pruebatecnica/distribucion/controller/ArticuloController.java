package com.pruebatecnica.distribucion.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.pruebatecnica.distribucion.dto.articulo.ArticuloCreateRequest;
import com.pruebatecnica.distribucion.dto.articulo.ArticuloDetailResponse;
import com.pruebatecnica.distribucion.dto.articulo.ArticuloSummaryResponse;
import com.pruebatecnica.distribucion.dto.articulo.ArticuloUpdateRequest;
import com.pruebatecnica.distribucion.service.ArticuloService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/articulos")
public class ArticuloController {

    private final ArticuloService articuloService;

    public ArticuloController(ArticuloService articuloService) {
        this.articuloService = articuloService;
    }

    @GetMapping
    public ResponseEntity<List<ArticuloSummaryResponse>> findAll() {
        return ResponseEntity.ok(articuloService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticuloDetailResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ArticuloSummaryResponse> create(@Valid @RequestBody ArticuloCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articuloService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticuloSummaryResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody ArticuloUpdateRequest request
    ) {
        return ResponseEntity.ok(articuloService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articuloService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
