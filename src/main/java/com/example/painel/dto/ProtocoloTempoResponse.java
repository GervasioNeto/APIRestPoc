package com.example.painel.dto;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;

public record ProtocoloTempoResponse(
        Risco risco,
        TipoAtendimento tipo,
        Integer tempoMaximoMinutos
) {
}
