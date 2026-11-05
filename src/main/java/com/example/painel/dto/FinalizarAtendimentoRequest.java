package com.example.painel.dto;

import com.example.painel.enums.ComportamentoSuicida;
import jakarta.validation.constraints.NotNull;

public record FinalizarAtendimentoRequest(
        @NotNull(message = "comportamentoSuicida é obrigatório para finalizar a consulta")
        ComportamentoSuicida comportamentoSuicida
) {
}
