package com.example.painel.dto;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Indicadores 4-27 suportados pelo repository, sem dados pessoais.
 * Os indicadores 1-3 não integram este contrato: faltam consultas agregadas.
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
        Map<Risco, Long> contagensPorRisco,
        List<Detalhamento> porRiscoETipo
) {
    public record Detalhamento(
            Risco risco, TipoAtendimento tipo, Double tempoMedioEsperaMinutos,
            long totalComProtocolo, long foraDoPrazo, BigDecimal percentualForaDoPrazo
    ) {}
}
