package com.example.painel.repository;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PacienteRepositoryIntegrationTests {

    @Autowired
    private PacienteRepository repository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Test
    @DisplayName("Só o primeiro consultório reserva o paciente; o segundo não sobrescreve")
    void reservaParaConsultorioSoGravaParaOPrimeiro() {
        Paciente paciente = salvar("Disputado por dois médicos", Risco.LARANJA, LocalDateTime.of(2026, 9, 1, 10, 0));
        Consultorio primeiro = consultorio(901L);
        Consultorio segundo = consultorio(902L);

        assertThat(repository.reservarParaConsultorio(paciente.getId(), primeiro)).isEqualTo(1);
        assertThat(repository.reservarParaConsultorio(paciente.getId(), segundo)).isZero();

        assertThat(repository.findById(paciente.getId()).orElseThrow().getConsultorio().getId())
                .isEqualTo(primeiro.getId());
    }

    @Test
    @DisplayName("Fila do médico: primeiro a cor e, na mesma cor, quem chegou antes (não quem foi cadastrado antes)")
    void filaOrdenaPelaCorEDepoisPelaChegada() {
        // Cadastrados nesta ordem (ids crescentes), mas com as chegadas fora de ordem.
        Paciente verdeChegouDepois = salvar("Verde que chegou depois", Risco.VERDE, LocalDateTime.of(2026, 9, 2, 16, 10));
        Paciente verdeChegouAntes = salvar("Verde que chegou antes", Risco.VERDE, LocalDateTime.of(2026, 8, 21, 9, 50));
        Paciente vermelho = salvar("Vermelho que chegou por último", Risco.VERMELHO, LocalDateTime.of(2026, 9, 3, 8, 0));
        Paciente amarelo = salvar("Amarelo", Risco.AMARELO, LocalDateTime.of(2026, 9, 1, 12, 0));

        assertThat(filaSomente(verdeChegouDepois, verdeChegouAntes, vermelho, amarelo))
                .extracting(Paciente::getNome)
                .containsExactly("Vermelho que chegou por último", "Amarelo", "Verde que chegou antes", "Verde que chegou depois");
    }

    @Test
    @DisplayName("Mesma cor e mesma chegada: desempata pela ordem de cadastro")
    void mesmaCorEMesmaChegadaDesempataPeloCadastro() {
        LocalDateTime chegada = LocalDateTime.of(2026, 9, 1, 10, 0);
        Paciente primeiro = salvar("Cadastrado primeiro", Risco.LARANJA, chegada);
        Paciente segundo = salvar("Cadastrado depois", Risco.LARANJA, chegada);

        assertThat(filaSomente(segundo, primeiro))
                .extracting(Paciente::getNome)
                .containsExactly("Cadastrado primeiro", "Cadastrado depois");
    }

    @Test
    @DisplayName("Fila do acolhimento: só quem ainda não foi classificado e não saiu do fluxo")
    void filaAcolhimentoMostraSoQuemAindaNaoFoiClassificado() {
        LocalDateTime chegada = LocalDateTime.of(2026, 9, 1, 10, 0);
        Paciente aguardando = salvarNoAcolhimento("Aguardando acolhimento", TipoStatus.AGUARDANDO_TRIAGEM, null, chegada);
        Paciente chamado = salvarNoAcolhimento("Chamado, ainda sem classificação", TipoStatus.CHAMADO, null, chegada.plusMinutes(1));
        Paciente finalizado = salvarNoAcolhimento("Finalizado", TipoStatus.FINALIZADO, null, chegada.plusMinutes(2));
        Paciente desistente = salvarNoAcolhimento("Desistente", TipoStatus.DESISTENCIA, null, chegada.plusMinutes(3));
        Paciente classificado = salvarNoAcolhimento("Já classificado", TipoStatus.AGUARDANDO_CONSULTA, Risco.LARANJA, chegada.plusMinutes(4));

        assertThat(filaAcolhimentoSomente(aguardando, chamado, finalizado, desistente, classificado))
                .extracting(Paciente::getNome)
                .containsExactly("Aguardando acolhimento", "Chamado, ainda sem classificação");
    }

    @Test
    @DisplayName("Fila do acolhimento: ordena pela chegada, não pela ordem de cadastro")
    void filaAcolhimentoOrdenaPelaChegada() {
        // Cadastrados nesta ordem (ids crescentes), mas com as chegadas fora de ordem.
        Paciente chegouPorUltimo = salvarNoAcolhimento("Chegou por último", TipoStatus.AGUARDANDO_TRIAGEM, null, LocalDateTime.of(2026, 9, 2, 16, 0));
        Paciente chegouPrimeiro = salvarNoAcolhimento("Chegou primeiro", TipoStatus.AGUARDANDO_TRIAGEM, null, LocalDateTime.of(2026, 9, 1, 8, 0));
        Paciente chegouNoMeio = salvarNoAcolhimento("Chegou no meio", TipoStatus.CHAMADO, null, LocalDateTime.of(2026, 9, 1, 12, 0));

        assertThat(filaAcolhimentoSomente(chegouPorUltimo, chegouPrimeiro, chegouNoMeio))
                .extracting(Paciente::getNome)
                .containsExactly("Chegou primeiro", "Chegou no meio", "Chegou por último");
    }

    @Test
    @DisplayName("Fila do acolhimento: mesma chegada desempata pela ordem de cadastro")
    void filaAcolhimentoMesmaChegadaDesempataPeloCadastro() {
        LocalDateTime chegada = LocalDateTime.of(2026, 9, 1, 10, 0);
        Paciente primeiro = salvarNoAcolhimento("Cadastrado primeiro", TipoStatus.AGUARDANDO_TRIAGEM, null, chegada);
        Paciente segundo = salvarNoAcolhimento("Cadastrado depois", TipoStatus.AGUARDANDO_TRIAGEM, null, chegada);

        assertThat(filaAcolhimentoSomente(segundo, primeiro))
                .extracting(Paciente::getNome)
                .containsExactly("Cadastrado primeiro", "Cadastrado depois");
    }

    @Test
    @DisplayName("Fila do acolhimento: sem status e sem risco continua na fila, como no filtro antigo")
    void filaAcolhimentoMantemPacienteSemStatus() {
        Paciente semStatus = salvarNoAcolhimento("Sem status", null, null, LocalDateTime.of(2026, 9, 1, 10, 0));

        assertThat(filaAcolhimentoSomente(semStatus))
                .extracting(Paciente::getNome)
                .containsExactly("Sem status");
    }

    // O banco de teste pode ter outros pacientes na fila; compara só a ordem relativa dos criados aqui.
    private List<Paciente> filaSomente(Paciente... pacientes) {
        List<Long> ids = java.util.Arrays.stream(pacientes).map(Paciente::getId).toList();
        return repository.buscarFilaDeEsperaOrdenada().stream()
                .filter(p -> ids.contains(p.getId()))
                .toList();
    }

    private Consultorio consultorio(Long numero) {
        Consultorio consultorio = new Consultorio();
        consultorio.setNumero(numero);
        return consultorioRepository.saveAndFlush(consultorio);
    }

    private Paciente salvar(String nome, Risco risco, LocalDateTime chegada) {
        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        paciente.setRisco(risco);
        paciente.setTipo(TipoAtendimento.PSIQUIATRICO);
        paciente.setChegadaAt(chegada);
        paciente.setClassifiedAt(chegada.plusMinutes(5));
        return repository.saveAndFlush(paciente);
    }

    // Mesmo cuidado do filaSomente: o banco de teste pode ter outros pacientes no acolhimento.
    private List<Paciente> filaAcolhimentoSomente(Paciente... pacientes) {
        List<Long> ids = java.util.Arrays.stream(pacientes).map(Paciente::getId).toList();
        return repository.buscarFilaDeAcolhimentoOrdenada().stream()
                .filter(p -> ids.contains(p.getId()))
                .toList();
    }

    private Paciente salvarNoAcolhimento(String nome, TipoStatus status, Risco risco, LocalDateTime chegada) {
        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setStatus(status);
        paciente.setRisco(risco);
        paciente.setChegadaAt(chegada);
        return repository.saveAndFlush(paciente);
    }
}
