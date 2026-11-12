package com.example.painel.dto;

import com.example.painel.enums.ComportamentoSuicida;
import com.example.painel.enums.Risco;
import jakarta.validation.constraints.NotNull;

public record FinalizarAtendimentoRequest(
        @NotNull(message = "comportamentoSuicida é obrigatório para finalizar a consulta")
        ComportamentoSuicida comportamentoSuicida,

        // Opcional: cor final definida pelo médico. Nula (ou igual à atual) mantém a cor da triagem.
        Risco riscoReclassificado
) {
}
