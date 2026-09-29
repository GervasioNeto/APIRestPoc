package com.example.painel.services;

import com.example.painel.repository.IndicadorRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IndicadorServiceTests {
    @Test void validaAntesDeConsultar() {
        var repository = mock(IndicadorRepository.class);
        var service = new IndicadorService(repository);
        var agora = LocalDateTime.now();
        assertThatThrownBy(() -> service.consultarIndicadores(null, agora)).isInstanceOf(IndicadorService.PeriodoInvalidoException.class);
        assertThatThrownBy(() -> service.consultarIndicadores(agora, null)).isInstanceOf(IndicadorService.PeriodoInvalidoException.class);
        assertThatThrownBy(() -> service.consultarIndicadores(agora, agora.minusSeconds(1))).isInstanceOf(IndicadorService.PeriodoInvalidoException.class);
        verifyNoInteractions(repository);
    }
    @Test void agregacoesNulasEPercentuaisDecimais() {
        var repository = mock(IndicadorRepository.class);
        var service = new IndicadorService(repository);
        var inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(repository.countCadastrados(inicio, inicio)).thenReturn(3L);
        when(repository.countClassificados(inicio, inicio)).thenReturn(1L);
        when(repository.countAtendidos(inicio, inicio)).thenReturn(2L);
        var contagem = mock(IndicadorRepository.RiscoContagem.class);
        when(contagem.getTotal()).thenReturn(null);
        when(contagem.getRisco()).thenReturn(com.example.painel.enums.Risco.VERMELHO);
        when(repository.contarPorRisco(inicio, inicio)).thenReturn(java.util.List.of(contagem));
        var media = mock(IndicadorRepository.TempoMedioPorRiscoTipo.class);
        when(media.getTempoMedioMinutos()).thenReturn(null);
        when(media.getRisco()).thenReturn("VERMELHO");
        when(media.getTipo()).thenReturn("CLINICO");
        when(repository.tempoMedioEsperaPorRiscoETipo(inicio, inicio)).thenReturn(java.util.List.of(media));
        var protocolo = mock(IndicadorRepository.ForaDoProtocoloPorRiscoTipo.class);
        when(protocolo.getTotal()).thenReturn(null);
        when(protocolo.getForaDoPrazo()).thenReturn(null);
        when(protocolo.getRisco()).thenReturn("VERMELHO");
        when(protocolo.getTipo()).thenReturn("CLINICO");
        when(repository.contarForaDoProtocoloPorRiscoETipo(inicio, inicio)).thenReturn(java.util.List.of(protocolo));
        var r = service.consultarIndicadores(inicio, inicio);
        assertThat(r.percentualClassificados()).isEqualByComparingTo("33.33");
        assertThat(r.percentualAtendidos()).isEqualByComparingTo("66.67");
        assertThat(r.contagensPorRisco().get(com.example.painel.enums.Risco.VERMELHO)).isZero();
        assertThat(r.porRiscoETipo()).allSatisfy(g -> {
            assertThat(g.tempoMedioEsperaMinutos()).isNull();
            assertThat(g.totalComProtocolo()).isZero();
            assertThat(g.foraDoPrazo()).isZero();
            assertThat(g.percentualForaDoPrazo()).isZero();
        });
        when(protocolo.getTotal()).thenReturn(3L);
        when(protocolo.getForaDoPrazo()).thenReturn(1L);
        assertThat(service.consultarIndicadores(inicio, inicio).porRiscoETipo().getFirst()
                .percentualForaDoPrazo()).isEqualByComparingTo("33.33");
    }

    @Test void semDenominadorRetornaZero() {
        var service = new IndicadorService(mock(IndicadorRepository.class));
        var agora = LocalDateTime.now();
        var r = service.consultarIndicadores(agora, agora);
        assertThat(r.percentualClassificados()).isZero();
        assertThat(r.percentualAtendidos()).isZero();
    }
}
