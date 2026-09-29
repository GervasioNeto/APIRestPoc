package com.example.painel.controllers;

import com.example.painel.dto.IndicadoresResponse;
import com.example.painel.services.IndicadorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GET /indicadores?inicio=2026-09-01T00:00:00&amp;fim=2026-09-09T23:59:59
 * Datas locais ISO obrigatórias. Responde 200 mesmo sem registros (contagens e
 * percentuais zero, médias null); parâmetros inválidos ou período invertido: 400.
 * Falhas de persistência não são convertidas em erro de validação.
 */
@RestController
@RequestMapping("/indicadores")
public class IndicadorController {
    private final IndicadorService service;

    public IndicadorController(IndicadorService service) { this.service = service; }

    @GetMapping
    public IndicadoresResponse consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return service.consultarIndicadores(inicio, fim);
    }

    @ExceptionHandler(IndicadorService.PeriodoInvalidoException.class)
    public ResponseEntity<Map<String, String>> periodoInvalido(IndicadorService.PeriodoInvalidoException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }
}
