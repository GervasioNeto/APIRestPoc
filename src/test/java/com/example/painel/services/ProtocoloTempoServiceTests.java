package com.example.painel.services;

import com.example.painel.dto.ProtocoloTempoResponse;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.repository.ProtocoloTempoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private ProtocoloTempo protocolo(Risco risco, TipoAtendimento tipo, Integer tempoMaximoMinutos) {
        ProtocoloTempo protocolo = new ProtocoloTempo();
        protocolo.setRisco(risco);
        protocolo.setTipo(tipo);
        protocolo.setTempoMaximoMinutos(tempoMaximoMinutos);
        return protocolo;
    }
}
