package com.pruebatecnica.distribucion.controller;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Map.of(
            "mensaje", "El proyecto Spring Boot está funcionando",
            "estado", "OK",
            "timestamp", OffsetDateTime.now().toString()
        ));
    }
}