package com.example.painel.dto;

import java.time.LocalDateTime;

public record HistoricoChamadaResponse(
        String tipo,
        String destino,
        LocalDateTime criadaEm
) {
}
