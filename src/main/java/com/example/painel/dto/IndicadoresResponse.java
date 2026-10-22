package com.example.painel.dto;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Indicadores 1-27 suportados pelo repository, sem dados pessoais.
 * Tempos 1-3 usam a 1ª chamada no painel (inclui quem foi chamado e não
 * compareceu) e consideram quem tem as duas datas, sem filtro de status.
 * Contagens por risco seguem a query (incluem NAO_CLASSIFICADO).
 * Detalhamento contém as 15 combinações das cinco cores com os três tipos.
 * Médias em minutos, sem arredondamento adicional; null significa sem amostras,
 * enquanto 0.0 representa espera média zero. Percentuais de 0 a 100, com duas
 * casas e HALF_UP; denominador zero produz 0.00.
 * totalComProtocolo é a população retornada pelo JOIN com protocolo_tempo,
 * não o total de cadastrados daquele risco/tipo.
 */
public record IndicadoresResponse(
        long cadastrados,
        long classificados,
        long atendidos,
        BigDecimal percentualClassificados,
        BigDecimal percentualAtendidos,
        Double tempoMedioAcolhimentoMinutos,
        Double tempoMedioClassificacaoAteMedicoMinutos,
        Double tempoMedioEntradaAteMedicoMinutos,
        Map<Risco, Long> contagensPorRisco,
        List<Detalhamento> porRiscoETipo
) {
    public record Detalhamento(
            Risco risco, TipoAtendimento tipo, Double tempoMedioEsperaMinutos,
            long totalComProtocolo, long foraDoPrazo, BigDecimal percentualForaDoPrazo
    ) {}
}
