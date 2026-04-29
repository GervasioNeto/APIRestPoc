package com.example.painel.controllers;

import com.example.painel.dto.ChamadaPainelResponse;
import com.example.painel.services.ChamadaPainelService;
import com.example.painel.services.PainelChamadasSseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/painel/chamadas")
public class PainelChamadasController {

    private final ChamadaPainelService chamadaPainelService;
    private final PainelChamadasSseService sseService;

    public PainelChamadasController(ChamadaPainelService chamadaPainelService,
                                    PainelChamadasSseService sseService) {
        this.chamadaPainelService = chamadaPainelService;
        this.sseService = sseService;
    }

    @GetMapping("/recentes")
    public ResponseEntity<List<ChamadaPainelResponse>> recentes() {
        return ResponseEntity.ok(chamadaPainelService.listarRecentes());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.subscribe();
    }
}
