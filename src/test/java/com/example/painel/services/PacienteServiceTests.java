package com.example.painel.services;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.ProtocoloTempoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTests {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private ConsultorioRepository consultorioRepository;

    @Mock
    private ChamadaPainelService chamadaPainelService;

    @Mock
    private ProtocoloTempoRepository protocoloTempoRepository;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    void primeiraChamadaParaTriagemRegistraHorario() {
        Paciente paciente = new Paciente();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.chamarParaTriagem(1L);

        assertThat(paciente.getChamadaTriagemAt()).isNotNull();
        assertThat(paciente.getStatus()).isEqualTo(TipoStatus.CHAMADO);
        verify(chamadaPainelService).registrarChamadaTriagem(paciente);
    }

    @Test
    void chamarParaTriagemNovamentePreservaHorarioDaPrimeiraChamada() {
        Paciente paciente = new Paciente();
        LocalDateTime primeiraChamada = LocalDateTime.of(2026, 9, 29, 10, 0);
        paciente.setChamadaTriagemAt(primeiraChamada);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.chamarParaTriagem(1L);
        pacienteService.chamarParaTriagem(1L);

        assertThat(paciente.getChamadaTriagemAt()).isEqualTo(primeiraChamada);
        verify(pacienteRepository, times(2)).save(paciente);
        verify(chamadaPainelService, times(2)).registrarChamadaTriagem(paciente);
    }

    @Test
    void primeiraChamadaParaConsultorioRegistraHorario() {
        Paciente paciente = new Paciente();
        Consultorio consultorio = new Consultorio();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(consultorioRepository.findById(2L)).thenReturn(Optional.of(consultorio));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.chamarParaConsultorio(1L, 2L);

        assertThat(paciente.getChamadaConsultorioAt()).isNotNull();
        assertThat(paciente.getConsultorio()).isSameAs(consultorio);
        assertThat(paciente.getStatus()).isEqualTo(TipoStatus.CHAMADO);
        verify(chamadaPainelService).registrarChamadaConsultorio(paciente, consultorio);
    }

    @Test
    void chamarParaConsultorioNovamentePreservaHorarioDaPrimeiraChamada() {
        Paciente paciente = new Paciente();
        Consultorio consultorio = new Consultorio();
        LocalDateTime primeiraChamada = LocalDateTime.of(2026, 9, 29, 10, 0);
        paciente.setChamadaConsultorioAt(primeiraChamada);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(consultorioRepository.findById(2L)).thenReturn(Optional.of(consultorio));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.chamarParaConsultorio(1L, 2L);
        pacienteService.chamarParaConsultorio(1L, 2L);

        assertThat(paciente.getChamadaConsultorioAt()).isEqualTo(primeiraChamada);
        verify(pacienteRepository, times(2)).save(paciente);
        verify(chamadaPainelService, times(2)).registrarChamadaConsultorio(paciente, consultorio);
    }

    @Test
    void rechamadasDeTriagemIncrementamSomenteSeuContadorEPreservamHorarioInicial() {
        Paciente paciente = new Paciente();
        LocalDateTime primeiraChamada = LocalDateTime.of(2026, 9, 29, 10, 0);
        paciente.setStatus(TipoStatus.CHAMADO);
        paciente.setChamadaTriagemAt(primeiraChamada);
        paciente.setRechamadasConsultorioCount(3);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.rechamarTriagem(1L);
        assertThat(paciente.getRechamadasTriagemCount()).isEqualTo(1);
        pacienteService.rechamarTriagem(1L);

        assertThat(paciente.getRechamadasTriagemCount()).isEqualTo(2);
        assertThat(paciente.getRechamadasConsultorioCount()).isEqualTo(3);
        assertThat(paciente.getChamadaTriagemAt()).isEqualTo(primeiraChamada);
        verify(pacienteRepository, times(2)).save(paciente);
        verify(chamadaPainelService, times(2)).registrarChamadaTriagem(paciente);
    }

    @Test
    void rechamadaDeTriagemTrataContadorNuloComoZero() {
        Paciente paciente = new Paciente();
        paciente.setStatus(TipoStatus.CHAMADO);
        paciente.setChamadaTriagemAt(LocalDateTime.now());
        paciente.setRechamadasTriagemCount(null);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.rechamarTriagem(1L);

        assertThat(paciente.getRechamadasTriagemCount()).isEqualTo(1);
    }

    @Test
    void triagemSemPrimeiraChamadaOuJaEncerradaNaoPodeSerRechamada() {
        Paciente paciente = new Paciente();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> pacienteService.rechamarTriagem(1L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        paciente.setChamadaTriagemAt(LocalDateTime.now());
        paciente.setStatus(TipoStatus.FINALIZADO);
        assertThatThrownBy(() -> pacienteService.rechamarTriagem(1L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        paciente.setStatus(TipoStatus.CHAMADO);
        paciente.setRisco(com.example.painel.enums.Risco.VERDE);
        assertThatThrownBy(() -> pacienteService.rechamarTriagem(1L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);

        assertThat(paciente.getRechamadasTriagemCount()).isZero();
        verify(pacienteRepository, never()).save(any());
        verifyNoInteractions(chamadaPainelService);
    }

    @Test
    void cadaRechamadaIncrementaSomenteContadorDoConsultorioEPreservaChamadaInicial() {
        Paciente paciente = new Paciente();
        Consultorio consultorio = new Consultorio();
        LocalDateTime primeiraChamada = LocalDateTime.of(2026, 9, 29, 10, 0);
        paciente.setConsultorio(consultorio);
        paciente.setChamadaConsultorioAt(primeiraChamada);
        paciente.setRechamadasTriagemCount(3);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.rechamarPaciente(1L);
        assertThat(paciente.getRechamadasConsultorioCount()).isEqualTo(1);
        pacienteService.rechamarPaciente(1L);

        assertThat(paciente.getRechamadasConsultorioCount()).isEqualTo(2);
        assertThat(paciente.getRechamadasTriagemCount()).isEqualTo(3);
        assertThat(paciente.getConsultorio()).isSameAs(consultorio);
        assertThat(paciente.getChamadaConsultorioAt()).isEqualTo(primeiraChamada);
        assertThat(paciente.getStatus()).isEqualTo(TipoStatus.CHAMADO);
        verify(pacienteRepository, times(2)).save(paciente);
        verify(chamadaPainelService, times(2)).registrarChamadaConsultorio(paciente, consultorio);
    }

    @Test
    void rechamadaDeConsultorioTrataContadorNuloComoZero() {
        Paciente paciente = new Paciente();
        paciente.setConsultorio(new Consultorio());
        paciente.setRechamadasConsultorioCount(null);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        pacienteService.rechamarPaciente(1L);

        assertThat(paciente.getRechamadasConsultorioCount()).isEqualTo(1);
    }

    @Test
    void pacienteSemConsultorioNaoIncrementaContadorNemDisparaChamada() {
        Paciente paciente = new Paciente();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> pacienteService.rechamarPaciente(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Paciente não está em atendimento");

        assertThat(paciente.getRechamadasConsultorioCount()).isZero();
        verify(pacienteRepository, never()).save(any());
        verifyNoInteractions(chamadaPainelService);
    }

    @Test
    void filaMedicaCalculaPrazoPeloProtocoloCarregandoProtocoloUmaVez() {
        Paciente laranjaClinico = pacienteClassificado(Risco.LARANJA, TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 29, 10, 0));
        Paciente amareloPsiquiatrico = pacienteClassificado(Risco.AMARELO, TipoAtendimento.PSIQUIATRICO,
                LocalDateTime.of(2026, 9, 29, 10, 5));
        Paciente semProtocolo = pacienteClassificado(Risco.VERDE, TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 29, 10, 10));
        when(pacienteRepository.buscarFilaDeEsperaOrdenada())
                .thenReturn(List.of(laranjaClinico, amareloPsiquiatrico, semProtocolo));
        when(protocoloTempoRepository.findAll()).thenReturn(List.of(
                protocolo(Risco.LARANJA, TipoAtendimento.CLINICO, 10),
                protocolo(Risco.AMARELO, TipoAtendimento.PSIQUIATRICO, 60)));

        List<Paciente> fila = pacienteService.listarFilaMedica();

        assertThat(fila).containsExactly(laranjaClinico, amareloPsiquiatrico, semProtocolo);
        assertThat(laranjaClinico.getPrazoAtendimentoAt()).isEqualTo(LocalDateTime.of(2026, 9, 29, 10, 10));
        assertThat(amareloPsiquiatrico.getPrazoAtendimentoAt()).isEqualTo(LocalDateTime.of(2026, 9, 29, 11, 5));
        assertThat(semProtocolo.getPrazoAtendimentoAt()).isNull();
        verify(protocoloTempoRepository, times(1)).findAll();
        verify(protocoloTempoRepository, never()).findByRiscoAndTipo(any(), any());
    }

    @Test
    void filaMedicaSemClassificacaoOuTipoRetornaPrazoNulo() {
        Paciente semClassifiedAt = pacienteClassificado(Risco.LARANJA, TipoAtendimento.CLINICO, null);
        Paciente semTipo = pacienteClassificado(Risco.LARANJA, null, LocalDateTime.of(2026, 9, 29, 10, 0));
        when(pacienteRepository.buscarFilaDeEsperaOrdenada()).thenReturn(List.of(semClassifiedAt, semTipo));
        when(protocoloTempoRepository.findAll()).thenReturn(List.of(
                protocolo(Risco.LARANJA, TipoAtendimento.CLINICO, 10)));

        pacienteService.listarFilaMedica();

        assertThat(semClassifiedAt.getPrazoAtendimentoAt()).isNull();
        assertThat(semTipo.getPrazoAtendimentoAt()).isNull();
    }

    @Test
    void filaMedicaVaziaNaoConsultaProtocolo() {
        when(pacienteRepository.buscarFilaDeEsperaOrdenada()).thenReturn(List.of());

        assertThat(pacienteService.listarFilaMedica()).isEmpty();
        verifyNoInteractions(protocoloTempoRepository);
    }

    private Paciente pacienteClassificado(Risco risco, TipoAtendimento tipo, LocalDateTime classifiedAt) {
        Paciente paciente = new Paciente();
        paciente.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        paciente.setRisco(risco);
        paciente.setTipo(tipo);
        paciente.setClassifiedAt(classifiedAt);
        return paciente;
    }

    private ProtocoloTempo protocolo(Risco risco, TipoAtendimento tipo, int minutos) {
        ProtocoloTempo protocolo = new ProtocoloTempo();
        protocolo.setRisco(risco);
        protocolo.setTipo(tipo);
        protocolo.setTempoMaximoMinutos(minutos);
        return protocolo;
    }

    @Test
    void finalizarAtendimentoAnonimizaPacienteESuasChamadasNoPainel() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNome("Maria Silva");
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        pacienteService.finalizarAtendimento(1L);

        assertThat(paciente.getStatus()).isEqualTo(TipoStatus.FINALIZADO);
        assertThat(paciente.getNome()).isEqualTo("Paciente Anônimo");
        verify(pacienteRepository).save(paciente);
        verify(chamadaPainelService).anonimizarChamadasDoPaciente(1L, "Paciente Anônimo");
    }

    @Test
    void registrarDesistenciaAnonimizaPacienteESuasChamadasNoPainel() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNome("Maria Silva");
        paciente.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        pacienteService.registrarDesistencia(1L);

        assertThat(paciente.getStatus()).isEqualTo(TipoStatus.DESISTENCIA);
        assertThat(paciente.getEtapaDesistencia()).isEqualTo(TipoStatus.AGUARDANDO_CONSULTA);
        assertThat(paciente.getNome()).isEqualTo("Paciente Anônimo");
        verify(pacienteRepository).save(paciente);
        verify(chamadaPainelService).anonimizarChamadasDoPaciente(1L, "Paciente Anônimo");
    }
}
