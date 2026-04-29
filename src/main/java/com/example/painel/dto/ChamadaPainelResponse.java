package com.example.painel.dto;

import java.time.LocalDateTime;

public record ChamadaPainelResponse(
        Long id,
        Long pacienteId,
        String patientName,
        String ticketNumber,
        String type,
        String destination,
        Long consultorioId,
        Long consultorioNumero,
        String priority,
        LocalDateTime createdAt
) {
}
