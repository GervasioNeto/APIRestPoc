package com.example.painel.services;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class PacienteServiceTests
{
    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    ConsultorioRepository consultorioRepository;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    @DisplayName("Deve listar todos os pacientes com êxito.")
    void testeListarTodosOsPacientes() {
        Paciente p1 = new Paciente();
        Paciente p2 = new Paciente();

        List<Paciente> listaDePacientes = List.of(p1, p2);

        when(pacienteRepository.findAll()).thenReturn(listaDePacientes);

        List<Paciente> resultado = pacienteService.listarTodos();

        assertThat(resultado)
                .isNotNull()
                .hasSize(2)
                .containsExactly(p1, p2);


        verify(pacienteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar um paciente quando o ID existir no db.")
    void testeBuscarPorIdHappyPath() {
        Long id = 1L;
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setNome("Fulano");

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));

        Paciente resultado = pacienteService.buscarPorId(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getNome()).isEqualTo("Fulano");
        verify(pacienteRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Deve lançar uma exceção quando o paciente não for encontrado no db.")
    void testeBuscarPorIdUnhappyPath() {

        Long id = 2L;
        when(pacienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorId(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Paciente não encontrado com o ID: " + id);

        verify(pacienteRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Deve criar um paciente com sucesso")
    void testarCriarPaciente() {
        Paciente paciente = new Paciente();
        paciente.setNome("Fulano");
        paciente.setStatus(TipoStatus.FINALIZADO);
        paciente.setCpf("12345679809");

        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        Paciente resultado = pacienteService.criar(paciente);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Fulano");
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.AGUARDANDO_TRIAGEM);
        assertThat(resultado.getCpf()).isEqualTo("12345679809");

        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve deletar um paciente com sucesso.")
    void testarDeletarPacienteHappyPath() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNome("Fulano");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        pacienteService.deletar(1L);

        verify(pacienteRepository, times(1)).delete(paciente);
    }

    @Test
    @DisplayName("Deve lançar uma exceção e não deletar nada quando o ID não existir.")
    void testarDeletarPacienteUnhappyPath() {
        Long id = 2L;

        when(pacienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.deletar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Paciente não encontrado com o ID: " + id);

        verify(pacienteRepository, never()).delete(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve listar os pacientes que estão aguardando triagem com sucesso")
    void testarListarAguardandoTriagem() {
        Paciente p1 = new Paciente();
        p1.setNome("Fulano");
        p1.setStatus(TipoStatus.AGUARDANDO_TRIAGEM);
        p1.setRisco(null); // Garantir que o risco é nulo

        Paciente p2 = new Paciente();
        p2.setNome("Beltrano");
        p2.setStatus(TipoStatus.FINALIZADO);
        p2.setRisco(Risco.VERDE); // Definir um risco para garantir que ele seja filtrado

        List<Paciente> listaDePacientes = List.of(p1, p2);

        when(pacienteRepository.findAll()).thenReturn(listaDePacientes);

        List<Paciente> resultado = pacienteService.listarAguardandoTriagem();

        assertThat(resultado)
                .hasSize(1) // A lista final deve ter apenas 1 paciente
                .containsExactly(p1) // Deve ser especificamente o paciente sem risco
                .doesNotContain(p2);

        verify(pacienteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar um paciente classificado com sucesso.")
    void testarClassificarPaciente() {

        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setStatus(TipoStatus.AGUARDANDO_TRIAGEM);

        Paciente dados = new Paciente();
        dados.setRisco(Risco.AMARELO);
        dados.setTipo(TipoAtendimento.PSIQUIATRICO);
        dados.setTriageNotes("Paciente enfermo");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Paciente resultado = pacienteService.classificar(1L, dados);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getRisco()).isEqualTo(Risco.AMARELO);
        assertThat(resultado.getTipo()).isEqualTo(TipoAtendimento.PSIQUIATRICO);
        assertThat(resultado.getTriageNotes()).isEqualTo("Paciente enfermo");
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.AGUARDANDO_CONSULTA);
        assertThat(resultado.getClassifiedAt()).isNotNull();

        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve finalizar o atendimento de um paciente com sucesso.")
    void testarFinalizarAtendimento() {
        Long id = 1L;
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setStatus(TipoStatus.CHAMADO);

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Paciente resultado = pacienteService.finalizarAtendimento(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.FINALIZADO);
        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve registrar a desistência de um paciente com sucesso.")
    void testarRegistrarDesistencia() {
        Long id = 1L;
        Paciente paciente = new Paciente();
        paciente.setId(id);

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Paciente resultado = pacienteService.registrarDesistencia(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.DESISTENCIA);
        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve rechamar um paciente que já possui consultório vinculado.")
    void testarRechamarPacienteHappyPath() {
        Long id = 1L;
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setConsultorio(new Consultorio());

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Paciente resultado = pacienteService.rechamarPaciente(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.CHAMADO);
        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao rechamar um paciente sem consultório vinculado.")
    void testarRechamarPacienteUnhappyPath() {
        Long id = 1L;
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setConsultorio(null);

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> pacienteService.rechamarPaciente(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Paciente não está em atendimento");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve recolocar o paciente na fila de consulta removendo o consultório.")
    void testarRecolocarNaFila() {
        Long id = 1L;
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setConsultorio(new Consultorio());

        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Paciente resultado = pacienteService.recolocarNaFila(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getConsultorio()).isNull();
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.AGUARDANDO_CONSULTA);
        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve listar a fila médica ordenada com sucesso.")
    void testarListarFilaMedica() {
        Paciente p1 = new Paciente();
        p1.setNome("Urgente");
        List<Paciente> fila = List.of(p1);

        when(pacienteRepository.buscarFilaDeEsperaOrdenada()).thenReturn(fila);

        List<Paciente> resultado = pacienteService.listarFilaMedica();

        assertThat(resultado).hasSize(1).containsExactly(p1);
        verify(pacienteRepository, times(1)).buscarFilaDeEsperaOrdenada();
    }

    @Test
    @DisplayName("Deve chamar um paciente para o consultório com sucesso.")
    void testarChamarParaConsultorioHappyPath() {
        Long pacienteId = 1L;
        Long consultorioId = 2L;

        Paciente paciente = new Paciente();
        paciente.setId(pacienteId);

        Consultorio consultorio = new Consultorio();
        consultorio.setId(consultorioId);

        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(consultorioRepository.findById(consultorioId)).thenReturn(Optional.of(consultorio));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));

        Paciente resultado = pacienteService.chamarParaConsultorio(pacienteId, consultorioId);

        assertThat(resultado.getConsultorio()).isEqualTo(consultorio);
        assertThat(resultado.getStatus()).isEqualTo(TipoStatus.CHAMADO);
        verify(pacienteRepository).save(paciente);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o consultório não for encontrado ao chamar paciente.")
    void testarChamarParaConsultorioUnhappyPathConsultorioInexistente() {
        Long pacienteId = 1L;
        Long consultorioId = 2L;

        Paciente paciente = new Paciente();
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(consultorioRepository.findById(consultorioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.chamarParaConsultorio(pacienteId, consultorioId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Consultório não encontrado");

        verify(pacienteRepository, never()).save(any());
    }


}
