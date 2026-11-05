package com.example.painel.dto;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoChamadaPainel;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Paciente atendido (FINALIZADO) no período, sem dados pessoais: o nome já foi
 * anonimizado quando o atendimento terminou. chamadaConsultorioAt é a 1ª chamada
 * ao consultório. Rechamadas vêm de chamada_painel: todas as chamadas de cada
 * tipo depois da primeira, em ordem de horário.
 */
public record AtendidoPorHorarioResponse(
        Long pacienteId,
        Risco risco,
        TipoAtendimento tipo,
        LocalDateTime chamadaConsultorioAt,
        List<Rechamada> rechamadas
) {
    public record Rechamada(TipoChamadaPainel tipo, LocalDateTime criadaEm) {}
}
