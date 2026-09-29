package com.example.painel.services;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTests {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private ChamadaPainelService chamadaPainelService;

    @InjectMocks
    private PacienteService pacienteService;

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
}
