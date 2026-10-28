package com.example.painel.controllers;

import com.example.painel.dto.ProtocoloTempoResponse;
import com.example.painel.services.ProtocoloTempoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /protocolos: tempo máximo de espera de cada risco por tipo de atendimento (tabela protocolo_tempo).
 * O front usa para saber quais cores cada tipo aceita e o tempo de cada uma.
 */
@RestController
@RequestMapping("/protocolos")
public class ProtocoloTempoController {
    private final ProtocoloTempoService service;

    public ProtocoloTempoController(ProtocoloTempoService service) { this.service = service; }

    @GetMapping
    public List<ProtocoloTempoResponse> listar() {
        return service.listar();
    }
}
