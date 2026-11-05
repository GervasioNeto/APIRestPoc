package com.example.painel.controllers;

import com.example.painel.dto.AtendidoPorHorarioResponse;
import com.example.painel.enums.EscopoIndicadores;
import com.example.painel.services.AtendidosPorHorarioService;
import com.example.painel.services.IndicadorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GET /indicadores/atendidos?inicio=2026-09-01T00:00:00&amp;fim=2026-09-03T23:59:59&amp;escopo=GERAL
 * Mesmos parâmetros e regras de 400 do GET /indicadores. Sem registros: lista vazia.
 */
@RestController
@RequestMapping("/indicadores/atendidos")
public class AtendidosPorHorarioController {
    private final AtendidosPorHorarioService service;

    public AtendidosPorHorarioController(AtendidosPorHorarioService service) { this.service = service; }

    @GetMapping
    public List<AtendidoPorHorarioResponse> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "GERAL") EscopoIndicadores escopo) {
        return service.listar(inicio, fim, escopo);
    }

    @ExceptionHandler(IndicadorService.PeriodoInvalidoException.class)
    public ResponseEntity<Map<String, String>> periodoInvalido(IndicadorService.PeriodoInvalidoException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }
}
