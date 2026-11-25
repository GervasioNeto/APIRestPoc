package com.example.painel.services;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.ComportamentoSuicida;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.ProtocoloTempoRepository;
import com.example.painel.services.PacienteService.Etapa;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// BE-04: cada ação do paciente só vale na sua etapa. Fora dela: 409, nada gravado e nada no painel.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PacienteEtapaTests {

    @Mock PacienteRepository pacienteRepository;
    @Mock ConsultorioRepository consultorioRepository;
    @Mock ChamadaPainelService chamadaPainelService;
    @Mock ProtocoloTempoRepository protocoloTempoRepository;
    @InjectMocks PacienteService service;

    enum Acao {
        CHAMAR_TRIAGEM((s, id) -> s.chamarParaTriagem(id),
                EnumSet.of(Etapa.AGUARDANDO_ACOLHIMENTO, Etapa.CHAMADO_ACOLHIMENTO)),
        CLASSIFICAR((s, id) -> s.classificar(id, dadosDaClassificacao()),
                EnumSet.of(Etapa.CHAMADO_ACOLHIMENTO, Etapa.AGUARDANDO_CONSULTA)),
        CHAMAR_CONSULTORIO((s, id) -> s.chamarParaConsultorio(id, 9L),
                EnumSet.of(Etapa.AGUARDANDO_CONSULTA)),
        RECHAMAR_CONSULTORIO((s, id) -> s.rechamarPaciente(id),
                EnumSet.of(Etapa.CHAMADO_CONSULTORIO)),
        FINALIZAR((s, id) -> s.finalizarAtendimento(id, ComportamentoSuicida.NENHUM, null),
                EnumSet.of(Etapa.CHAMADO_CONSULTORIO)),
        DESISTENCIA((s, id) -> s.registrarDesistencia(id),
                EnumSet.of(Etapa.AGUARDANDO_ACOLHIMENTO, Etapa.CHAMADO_ACOLHIMENTO,
                        Etapa.AGUARDANDO_CONSULTA, Etapa.CHAMADO_CONSULTORIO));

        final BiConsumer<PacienteService, Long> executar;
        final Set<Etapa> aceitas;

        Acao(BiConsumer<PacienteService, Long> executar, Set<Etapa> aceitas) {
            this.executar = executar;
            this.aceitas = aceitas;
        }
    }

    static Stream<Arguments> recusadas() {
        return Arrays.stream(Acao.values()).flatMap(acao -> Arrays.stream(Etapa.values())
                .filter(etapa -> !acao.aceitas.contains(etapa))
                // Já chamado por outro consultório tem o 409 da reserva, coberto em PacienteServiceTests.
                .filter(etapa -> !(acao == Acao.CHAMAR_CONSULTORIO && etapa == Etapa.CHAMADO_CONSULTORIO))
                .map(etapa -> Arguments.of(acao, etapa)));
    }

    static Stream<Arguments> aceitas() {
        return Arrays.stream(Acao.values())
                .flatMap(acao -> acao.aceitas.stream().map(etapa -> Arguments.of(acao, etapa)));
    }

    @ParameterizedTest(name = "{0} em {1} → 409")
    @MethodSource("recusadas")
    void acaoForaDaEtapaRetorna409SemGravarNemChamarNoPainel(Acao acao, Etapa etapa) {
        Paciente paciente = pacienteNaEtapa(etapa);
        TipoStatus statusAntes = paciente.getStatus();

        assertThatThrownBy(() -> acao.executar.accept(service, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException erro = (ResponseStatusException) e;
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(erro.getReason()).startsWith("Não é possível").endsWith("Atualize a tela.");
                });

        assertThat(paciente.getStatus()).isEqualTo(statusAntes);
        verify(pacienteRepository, never()).save(any());
        verify(pacienteRepository, never()).reservarParaConsultorio(anyLong(), any());
        verifyNoInteractions(chamadaPainelService);
    }

    @ParameterizedTest(name = "{0} em {1} → aceita")
    @MethodSource("aceitas")
    void acaoNaEtapaCertaContinuaFuncionando(Acao acao, Etapa etapa) {
        pacienteNaEtapa(etapa);

        assertThatCode(() -> acao.executar.accept(service, 1L)).doesNotThrowAnyException();
        verify(pacienteRepository, atLeastOnce()).save(any());
    }

    @ParameterizedTest(name = "{0} gera a etapa {1}")
    @MethodSource("etapasDoPaciente")
    void etapaVemDoStatusEDoConsultorio(TipoStatus status, boolean comConsultorio, Etapa esperada) {
        Paciente paciente = new Paciente();
        paciente.setStatus(status);
        if (comConsultorio) paciente.setConsultorio(new Consultorio());

        assertThat(PacienteService.etapaDe(paciente)).isEqualTo(esperada);
    }

    static Stream<Arguments> etapasDoPaciente() {
        return Stream.of(
                Arguments.of(TipoStatus.AGUARDANDO_TRIAGEM, false, Etapa.AGUARDANDO_ACOLHIMENTO),
                Arguments.of(TipoStatus.CHAMADO, false, Etapa.CHAMADO_ACOLHIMENTO),
                Arguments.of(TipoStatus.AGUARDANDO_CONSULTA, false, Etapa.AGUARDANDO_CONSULTA),
                Arguments.of(TipoStatus.CHAMADO, true, Etapa.CHAMADO_CONSULTORIO),
                Arguments.of(TipoStatus.FINALIZADO, true, Etapa.FINALIZADO),
                Arguments.of(TipoStatus.DESISTENCIA, false, Etapa.DESISTIU));
    }

    private Paciente pacienteNaEtapa(Etapa etapa) {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setRisco(Risco.LARANJA);
        p.setTipo(TipoAtendimento.CLINICO);
        switch (etapa) {
            case AGUARDANDO_ACOLHIMENTO -> { p.setStatus(TipoStatus.AGUARDANDO_TRIAGEM); p.setRisco(null); p.setTipo(null); }
            case CHAMADO_ACOLHIMENTO -> { p.setStatus(TipoStatus.CHAMADO); p.setRisco(null); p.setTipo(null); }
            case AGUARDANDO_CONSULTA -> p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
            case CHAMADO_CONSULTORIO -> { p.setStatus(TipoStatus.CHAMADO); p.setConsultorio(new Consultorio()); }
            case FINALIZADO -> { p.setStatus(TipoStatus.FINALIZADO); p.setConsultorio(new Consultorio()); }
            case DESISTIU -> p.setStatus(TipoStatus.DESISTENCIA);
        }
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(p));
        when(pacienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consultorioRepository.findById(9L)).thenReturn(Optional.of(new Consultorio()));
        when(pacienteRepository.reservarParaConsultorio(anyLong(), any())).thenReturn(1);
        return p;
    }

    private static Paciente dadosDaClassificacao() {
        Paciente dados = new Paciente();
        dados.setRisco(Risco.LARANJA);
        dados.setTipo(TipoAtendimento.CLINICO);
        return dados;
    }
}
