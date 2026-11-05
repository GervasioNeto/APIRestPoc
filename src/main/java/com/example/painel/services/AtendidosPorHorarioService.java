package com.example.painel.services;

import com.example.painel.dto.AtendidoPorHorarioResponse;
import com.example.painel.dto.AtendidoPorHorarioResponse.Rechamada;
import com.example.painel.entinty.ChamadaPainel;
import com.example.painel.enums.EscopoIndicadores;
import com.example.painel.enums.TipoChamadaPainel;
import com.example.painel.repository.ChamadaPainelRepository;
import com.example.painel.repository.IndicadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AtendidosPorHorarioService {
    private final IndicadorRepository indicadorRepository;
    private final ChamadaPainelRepository chamadaPainelRepository;

    public AtendidosPorHorarioService(IndicadorRepository indicadorRepository,
                                      ChamadaPainelRepository chamadaPainelRepository) {
        this.indicadorRepository = indicadorRepository;
        this.chamadaPainelRepository = chamadaPainelRepository;
    }

    /**
     * Atendidos com o mesmo período (chegadaAt, inclusivo) e escopo do GET /indicadores,
     * ordenados pela 1ª chamada ao consultório. O filtro por faixa de hora fica no front.
     */
    @Transactional(readOnly = true)
    public List<AtendidoPorHorarioResponse> listar(LocalDateTime inicio, LocalDateTime fim, EscopoIndicadores escopo) {
        if (escopo == null) throw new IllegalArgumentException("escopo é obrigatório");
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IndicadorService.PeriodoInvalidoException();
        }
        var atendidos = indicadorRepository.listarAtendidos(inicio, fim, escopo == EscopoIndicadores.SAMU);
        if (atendidos.isEmpty()) return List.of();

        Map<Long, List<ChamadaPainel>> chamadasPorPaciente = chamadaPainelRepository
                .findByPacienteIdInOrderByCriadaEmAscIdAsc(atendidos.stream().map(IndicadorRepository.AtendidoResumo::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ChamadaPainel::getPacienteId));

        return atendidos.stream()
                .map(a -> new AtendidoPorHorarioResponse(a.getId(), a.getRisco(), a.getTipo(), a.getChamadaConsultorioAt(),
                        rechamadas(chamadasPorPaciente.getOrDefault(a.getId(), List.of()))))
                .toList();
    }

    // A 1ª chamada de cada tipo é a chamada original; as seguintes são rechamadas.
    private static List<Rechamada> rechamadas(List<ChamadaPainel> chamadas) {
        Set<TipoChamadaPainel> jaChamado = EnumSet.noneOf(TipoChamadaPainel.class);
        List<Rechamada> rechamadas = new ArrayList<>();
        for (ChamadaPainel chamada : chamadas) {
            if (chamada.getTipo() == null) continue;
            if (!jaChamado.add(chamada.getTipo())) {
                rechamadas.add(new Rechamada(chamada.getTipo(), chamada.getCriadaEm()));
            }
        }
        return List.copyOf(rechamadas);
    }
}
