package com.example.painel.services;

import com.example.painel.dto.AtendidoPorHorarioResponse.Rechamada;
import com.example.painel.entinty.ChamadaPainel;
import com.example.painel.enums.EscopoIndicadores;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoChamadaPainel;
import com.example.painel.repository.ChamadaPainelRepository;
import com.example.painel.repository.IndicadorRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AtendidosPorHorarioServiceTests {
    final IndicadorRepository indicadorRepository = mock(IndicadorRepository.class);
    final ChamadaPainelRepository chamadaPainelRepository = mock(ChamadaPainelRepository.class);
    final AtendidosPorHorarioService service = new AtendidosPorHorarioService(indicadorRepository, chamadaPainelRepository);
    final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);

    @Test void validaAntesDeConsultar() {
        assertThatThrownBy(() -> service.listar(null, inicio, EscopoIndicadores.GERAL))
                .isInstanceOf(IndicadorService.PeriodoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(inicio, inicio.minusSeconds(1), EscopoIndicadores.GERAL))
                .isInstanceOf(IndicadorService.PeriodoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(inicio, inicio, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("escopo é obrigatório");
        verifyNoInteractions(indicadorRepository, chamadaPainelRepository);
    }

    @Test void semAtendidosNaoBuscaChamadas() {
        when(indicadorRepository.listarAtendidos(inicio, inicio, true)).thenReturn(List.of());
        assertThat(service.listar(inicio, inicio, EscopoIndicadores.SAMU)).isEmpty();
        verifyNoInteractions(chamadaPainelRepository);
    }

    @Test void rechamadasSaoAsChamadasDepoisDaPrimeiraDeCadaTipo() {
        var atendido = atendido(1L, Risco.VERMELHO, TipoAtendimento.CLINICO, inicio.withHour(8).withMinute(5));
        var semChamadas = atendido(2L, Risco.AZUL, TipoAtendimento.PSIQUIATRICO, inicio.withHour(9));
        when(indicadorRepository.listarAtendidos(inicio, inicio, false)).thenReturn(List.of(atendido, semChamadas));
        when(chamadaPainelRepository.findByPacienteIdInOrderByCriadaEmAscIdAsc(any())).thenReturn(List.of(
                chamada(1L, TipoChamadaPainel.TRIAGEM, 2),
                chamada(1L, TipoChamadaPainel.TRIAGEM, 3),
                chamada(1L, TipoChamadaPainel.CONSULTORIO, 5),
                chamada(1L, TipoChamadaPainel.CONSULTORIO, 7)));

        var r = service.listar(inicio, inicio, EscopoIndicadores.GERAL);

        assertThat(r).hasSize(2);
        assertThat(r.getFirst().pacienteId()).isEqualTo(1L);
        assertThat(r.getFirst().risco()).isEqualTo(Risco.VERMELHO);
        assertThat(r.getFirst().tipo()).isEqualTo(TipoAtendimento.CLINICO);
        assertThat(r.getFirst().chamadaConsultorioAt()).isEqualTo(inicio.withHour(8).withMinute(5));
        assertThat(r.getFirst().rechamadas()).containsExactly(
                new Rechamada(TipoChamadaPainel.TRIAGEM, inicio.withHour(8).withMinute(3)),
                new Rechamada(TipoChamadaPainel.CONSULTORIO, inicio.withHour(8).withMinute(7)));
        assertThat(r.get(1).rechamadas()).isEmpty();
        verify(chamadaPainelRepository).findByPacienteIdInOrderByCriadaEmAscIdAsc(List.of(1L, 2L));
    }

    private static IndicadorRepository.AtendidoResumo atendido(Long id, Risco risco, TipoAtendimento tipo, LocalDateTime chamada) {
        var a = mock(IndicadorRepository.AtendidoResumo.class);
        when(a.getId()).thenReturn(id);
        when(a.getRisco()).thenReturn(risco);
        when(a.getTipo()).thenReturn(tipo);
        when(a.getChamadaConsultorioAt()).thenReturn(chamada);
        return a;
    }

    private ChamadaPainel chamada(Long pacienteId, TipoChamadaPainel tipo, int minuto) {
        var c = new ChamadaPainel();
        c.setPacienteId(pacienteId);
        c.setTipo(tipo);
        c.setCriadaEm(inicio.withHour(8).withMinute(minuto));
        return c;
    }
}
