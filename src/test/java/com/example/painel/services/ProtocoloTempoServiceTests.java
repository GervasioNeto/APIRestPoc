package com.example.painel.services;

import com.example.painel.dto.ProtocoloTempoResponse;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.repository.ProtocoloTempoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocoloTempoServiceTests {

    @Mock
    private ProtocoloTempoRepository repository;

    @InjectMocks
    private ProtocoloTempoService service;

    @Test
    void listaOrdenadaPorTipoEPrioridadeDoRisco() {
        when(repository.findAll()).thenReturn(List.of(
                protocolo(Risco.AZUL, TipoAtendimento.PSIQUIATRICO, 240),
                protocolo(Risco.LARANJA, TipoAtendimento.SAMU, 10),
                protocolo(Risco.LARANJA, TipoAtendimento.CLINICO, 10),
                protocolo(Risco.VERMELHO, TipoAtendimento.PSIQUIATRICO, 0),
                protocolo(Risco.VERMELHO, TipoAtendimento.CLINICO, 0)));

        assertThat(service.listar()).containsExactly(
                new ProtocoloTempoResponse(Risco.VERMELHO, TipoAtendimento.CLINICO, 0),
                new ProtocoloTempoResponse(Risco.LARANJA, TipoAtendimento.CLINICO, 10),
                new ProtocoloTempoResponse(Risco.VERMELHO, TipoAtendimento.PSIQUIATRICO, 0),
                new ProtocoloTempoResponse(Risco.AZUL, TipoAtendimento.PSIQUIATRICO, 240),
                new ProtocoloTempoResponse(Risco.LARANJA, TipoAtendimento.SAMU, 10));
    }

    @Test
    void ignoraLinhasIncompletas() {
        when(repository.findAll()).thenReturn(List.of(
                protocolo(Risco.AMARELO, TipoAtendimento.PSIQUIATRICO, null),
                protocolo(null, TipoAtendimento.CLINICO, 10),
                protocolo(Risco.VERDE, TipoAtendimento.PSIQUIATRICO, 120)));

        assertThat(service.listar()).containsExactly(
                new ProtocoloTempoResponse(Risco.VERDE, TipoAtendimento.PSIQUIATRICO, 120));
    }

    @Test
    void tabelaVaziaRetornaListaVazia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void cadastraProtocoloPadraoQuandoTabelaVazia() {
        when(repository.count()).thenReturn(0L);
        ArgumentCaptor<List<ProtocoloTempo>> salvos = ArgumentCaptor.forClass(List.class);

        assertThat(service.cadastrarProtocoloPadraoSeVazio()).isEqualTo(8);

        verify(repository).saveAll(salvos.capture());
        assertThat(salvos.getValue())
                .extracting(ProtocoloTempo::getRisco, ProtocoloTempo::getTipo, ProtocoloTempo::getTempoMaximoMinutos)
                .containsExactlyInAnyOrder(
                        tuple(Risco.VERMELHO, TipoAtendimento.CLINICO, 0),
                        tuple(Risco.LARANJA, TipoAtendimento.CLINICO, 10),
                        tuple(Risco.VERMELHO, TipoAtendimento.PSIQUIATRICO, 0),
                        tuple(Risco.LARANJA, TipoAtendimento.PSIQUIATRICO, 10),
                        tuple(Risco.AMARELO, TipoAtendimento.PSIQUIATRICO, 60),
                        tuple(Risco.VERDE, TipoAtendimento.PSIQUIATRICO, 120),
                        tuple(Risco.AZUL, TipoAtendimento.PSIQUIATRICO, 240),
                        tuple(Risco.LARANJA, TipoAtendimento.SAMU, 10));
    }

    @Test
    void naoMexeNoProtocoloJaCadastrado() {
        when(repository.count()).thenReturn(3L);

        assertThat(service.cadastrarProtocoloPadraoSeVazio()).isZero();

        verify(repository, never()).saveAll(any());
    }

    private ProtocoloTempo protocolo(Risco risco, TipoAtendimento tipo, Integer tempoMaximoMinutos) {
        ProtocoloTempo protocolo = new ProtocoloTempo();
        protocolo.setRisco(risco);
        protocolo.setTipo(tipo);
        protocolo.setTempoMaximoMinutos(tempoMaximoMinutos);
        return protocolo;
    }
}
