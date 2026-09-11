package com.example.painel.services;

import com.example.painel.dto.IndicadoresResponse;
import com.example.painel.dto.IndicadoresResponse.Detalhamento;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.repository.IndicadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IndicadorService {
    private final IndicadorRepository repository;

    public IndicadorService(IndicadorRepository repository) {
        this.repository = repository;
    }

    /**
     * Filtra chegadaAt entre inicio e fim, ambos inclusivos, sem offset ou
     * período padrão. Datas iguais são válidas. Preserva os filtros individuais
     * de status do repository; não aplica filtro global de atendimentos encerrados.
     */
    @Transactional(readOnly = true)
    public IndicadoresResponse consultarIndicadores(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new PeriodoInvalidoException();
        }
        long cadastrados = repository.countCadastrados(inicio, fim);
        long classificados = repository.countClassificados(inicio, fim);
        long atendidos = repository.countAtendidos(inicio, fim);
        Map<Risco, Long> contagens = new EnumMap<>(Risco.class);
        for (Risco risco : Risco.values()) contagens.put(risco, 0L);
        for (var grupo : repository.contarPorRisco(inicio, fim)) {
            if (grupo.getRisco() != null) contagens.put(grupo.getRisco(), zero(grupo.getTotal()));
        }
        var medias = repository.tempoMedioEsperaPorRiscoETipo(inicio, fim);
        var protocolos = repository.contarForaDoProtocoloPorRiscoETipo(inicio, fim);
        List<Detalhamento> detalhes = new ArrayList<>();
        for (Risco risco : Risco.values()) {
            if (risco == Risco.NAO_CLASSIFICADO) continue;
            for (TipoAtendimento tipo : TipoAtendimento.values()) {
                Double media = medias.stream()
                        .filter(g -> risco.name().equals(g.getRisco()) && tipo.name().equals(g.getTipo()))
                        .findFirst().map(IndicadorRepository.TempoMedioPorRiscoTipo::getTempoMedioMinutos).orElse(null);
                var protocolo = protocolos.stream()
                        .filter(g -> risco.name().equals(g.getRisco()) && tipo.name().equals(g.getTipo()))
                        .findFirst().orElse(null);
                long total = protocolo == null ? 0 : zero(protocolo.getTotal());
                long fora = protocolo == null ? 0 : zero(protocolo.getForaDoPrazo());
                detalhes.add(new Detalhamento(risco, tipo, media, total, fora, percentual(fora, total)));
            }
        }
        return new IndicadoresResponse(
                cadastrados, classificados, atendidos,
                percentual(classificados, cadastrados), percentual(atendidos, cadastrados),
                Collections.unmodifiableMap(contagens), List.copyOf(detalhes));
    }

    private static long zero(Long valor) { return valor == null ? 0L : valor; }

    private static BigDecimal percentual(long numerador, long denominador) {
        return denominador == 0 ? new BigDecimal("0.00") : BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }

    public static class PeriodoInvalidoException extends IllegalArgumentException {
        public PeriodoInvalidoException() {
            super("inicio e fim são obrigatórios; inicio deve ser menor ou igual a fim");
        }
    }
}
