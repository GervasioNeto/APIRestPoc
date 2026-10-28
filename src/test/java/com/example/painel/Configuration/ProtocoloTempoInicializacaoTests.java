package com.example.painel.Configuration;

import com.example.painel.services.ProtocoloTempoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProtocoloTempoInicializacaoTests {

    private final ProtocoloTempoService service = mock(ProtocoloTempoService.class);
    private final ApplicationRunner runner = new ProtocoloTempoInicializacao().cadastrarProtocoloPadrao(service);

    @Test
    void cadastraOProtocoloPadraoAoSubir() throws Exception {
        when(service.cadastrarProtocoloPadraoSeVazio()).thenReturn(8);

        runner.run(new DefaultApplicationArguments());

        verify(service).cadastrarProtocoloPadraoSeVazio();
    }

    @Test
    void tabelaAindaInexistenteNaoImpedeAAplicacaoDeSubir() {
        when(service.cadastrarProtocoloPadraoSeVazio())
                .thenThrow(new InvalidDataAccessResourceUsageException("relation \"protocolo_tempo\" does not exist"));

        assertThatCode(() -> runner.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
