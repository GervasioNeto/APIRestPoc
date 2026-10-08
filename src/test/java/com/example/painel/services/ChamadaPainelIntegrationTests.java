package com.example.painel.services;

import com.example.painel.dto.ChamadaPainelResponse;
import com.example.painel.dto.HistoricoChamadaResponse;
import com.example.painel.entinty.ChamadaPainel;
import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoChamadaPainel;
import com.example.painel.repository.ChamadaPainelRepository;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChamadaPainelIntegrationTests {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private ChamadaPainelService chamadaPainelService;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private ChamadaPainelRepository chamadaPainelRepository;

    @Autowired
    private CapturingPainelChamadasSseService sseService;

    @BeforeEach
    void setUp() {
        chamadaPainelRepository.deleteAll();
        pacienteRepository.deleteAll();
        consultorioRepository.deleteAll();
        sseService.clear();
    }

    @Test
    void chamadaDeTriagemSalvaRegistroEPublicaEventoAposCommit() {
        Paciente paciente = criarPaciente("Maria Silva");

        pacienteService.chamarParaTriagem(paciente.getId());

        List<ChamadaPainel> chamadas = chamadaPainelRepository.findTop10ByOrderByCriadaEmDescIdDesc();

        assertThat(chamadas).hasSize(1);
        ChamadaPainel chamada = chamadas.get(0);
        assertThat(chamada.getPacienteId()).isEqualTo(paciente.getId());
        assertThat(chamada.getNomePaciente()).isEqualTo("Maria Silva");
        assertThat(chamada.getTipo()).isEqualTo(TipoChamadaPainel.TRIAGEM);
        assertThat(chamada.getDestino()).isEqualTo("Triagem");

        assertThat(sseService.published()).extracting(ChamadaPainelResponse::id)
                .containsExactly(chamada.getId());
    }

    @Test
    void chamadaDeConsultorioSalvaSnapshotComDestinoCorreto() {
        Paciente paciente = criarPacienteClassificado("Joao Souza", Risco.AMARELO);
        Consultorio consultorio = criarConsultorio(3L);

        pacienteService.chamarParaConsultorio(paciente.getId(), consultorio.getId());

        ChamadaPainel chamada = chamadaPainelRepository.findTop10ByOrderByCriadaEmDescIdDesc().get(0);

        assertThat(chamada.getPacienteId()).isEqualTo(paciente.getId());
        assertThat(chamada.getTipo()).isEqualTo(TipoChamadaPainel.CONSULTORIO);
        assertThat(chamada.getConsultorioId()).isEqualTo(consultorio.getId());
        assertThat(chamada.getConsultorioNumero()).isEqualTo(3L);
        assertThat(chamada.getDestino()).isEqualTo("Consultorio 3");
        assertThat(chamada.getRisco()).isEqualTo(Risco.AMARELO);
    }

    @Test
    void rechamadaCriaNovoRegistroParaOMesmoPaciente() {
        Paciente paciente = criarPacienteClassificado("Ana Lima", Risco.VERDE);
        Consultorio consultorio = criarConsultorio(2L);

        pacienteService.chamarParaConsultorio(paciente.getId(), consultorio.getId());
        pacienteService.rechamarPaciente(paciente.getId());

        List<ChamadaPainel> chamadas = chamadaPainelRepository.findTop10ByOrderByCriadaEmDescIdDesc();

        assertThat(chamadas).hasSize(2);
        assertThat(chamadas).allMatch(chamada -> chamada.getPacienteId().equals(paciente.getId()));
        assertThat(chamadas).allMatch(chamada -> chamada.getTipo() == TipoChamadaPainel.CONSULTORIO);
    }

    @Test
    void listarRecentesRetornaMaisRecentesPrimeiro() {
        chamadaPainelRepository.save(chamadaSalva("Primeira", LocalDateTime.now().minusMinutes(2)));
        chamadaPainelRepository.save(chamadaSalva("Segunda", LocalDateTime.now().minusMinutes(1)));

        List<ChamadaPainelResponse> recentes = chamadaPainelService.listarRecentes();

        assertThat(recentes).extracting(ChamadaPainelResponse::patientName)
                .containsExactly("Segunda", "Primeira");
    }

    @Test
    void historicoDoPacienteRetornaUmaTriagemETresConsultoriosEmOrdemCronologica() {
        Paciente paciente = criarPaciente("Carla Mendes");
        Paciente outro = criarPaciente("Outro Paciente");
        Consultorio consultorio = criarConsultorio(4L);

        pacienteService.chamarParaTriagem(paciente.getId());
        pacienteService.chamarParaTriagem(outro.getId());
        Paciente dados = new Paciente();
        dados.setRisco(Risco.AMARELO);
        dados.setTipo(TipoAtendimento.PSIQUIATRICO);
        pacienteService.classificar(paciente.getId(), dados);
        pacienteService.chamarParaConsultorio(paciente.getId(), consultorio.getId());
        pacienteService.rechamarPaciente(paciente.getId());
        pacienteService.rechamarPaciente(paciente.getId());

        List<HistoricoChamadaResponse> historico = pacienteService.listarHistoricoChamadas(paciente.getId());

        assertThat(historico).extracting(HistoricoChamadaResponse::tipo)
                .containsExactly("TRIAGEM", "CONSULTORIO", "CONSULTORIO", "CONSULTORIO");
        assertThat(historico).extracting(HistoricoChamadaResponse::destino)
                .containsExactly("Triagem", "Consultorio 4", "Consultorio 4", "Consultorio 4");
        assertThat(historico).extracting(HistoricoChamadaResponse::criadaEm)
                .isSortedAccordingTo(java.util.Comparator.naturalOrder());
    }

    @Test
    void finalizarAtendimentoAnonimizaTodasAsChamadasDoPaciente() {
        Paciente paciente = criarPacienteClassificado("Pedro Alves", Risco.LARANJA);
        Paciente outro = criarPaciente("Lucia Rocha");
        Consultorio consultorio = criarConsultorio(5L);

        pacienteService.chamarParaTriagem(paciente.getId());
        pacienteService.chamarParaTriagem(outro.getId());
        pacienteService.chamarParaConsultorio(paciente.getId(), consultorio.getId());
        pacienteService.rechamarPaciente(paciente.getId());

        pacienteService.finalizarAtendimento(paciente.getId());

        assertChamadasAnonimizadas(paciente.getId(), 3, "Pedro Alves");
        assertThat(chamadasDoPaciente(outro.getId()))
                .extracting(ChamadaPainel::getNomePaciente).containsExactly("Lucia Rocha");

        List<ChamadaPainelResponse> recentes = chamadaPainelService.listarRecentes();
        assertThat(recentes).hasSize(4);
        assertThat(recentes).extracting(ChamadaPainelResponse::patientName)
                .doesNotContain("Pedro Alves")
                .contains("Paciente Anônimo", "Lucia Rocha");
    }

    @Test
    void registrarDesistenciaAnonimizaTodasAsChamadasDoPaciente() {
        Paciente paciente = criarPaciente("Rita Gomes");
        Paciente outro = criarPaciente("Marcos Dias");

        pacienteService.chamarParaTriagem(paciente.getId());
        pacienteService.rechamarTriagem(paciente.getId());
        pacienteService.chamarParaTriagem(outro.getId());

        pacienteService.registrarDesistencia(paciente.getId());

        assertChamadasAnonimizadas(paciente.getId(), 2, "Rita Gomes");
        assertThat(chamadasDoPaciente(outro.getId()))
                .extracting(ChamadaPainel::getNomePaciente).containsExactly("Marcos Dias");

        assertThat(chamadaPainelService.listarRecentes())
                .extracting(ChamadaPainelResponse::patientName)
                .containsExactly("Marcos Dias", "Paciente Anônimo", "Paciente Anônimo");
    }

    private void assertChamadasAnonimizadas(Long pacienteId, int quantidade, String nomeReal) {
        List<ChamadaPainel> chamadas = chamadasDoPaciente(pacienteId);
        assertThat(chamadas).hasSize(quantidade);
        assertThat(chamadas).extracting(ChamadaPainel::getNomePaciente)
                .doesNotContain(nomeReal)
                .containsOnly("Paciente Anônimo");
    }

    private List<ChamadaPainel> chamadasDoPaciente(Long pacienteId) {
        return chamadaPainelRepository.findAll().stream()
                .filter(chamada -> chamada.getPacienteId().equals(pacienteId))
                .toList();
    }

    private Paciente criarPaciente(String nome) {
        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setCpf(nome.replace(" ", ".").toLowerCase());
        return pacienteService.criar(paciente);
    }

    private Paciente criarPacienteClassificado(String nome, Risco risco) {
        Paciente paciente = criarPaciente(nome);

        Paciente dados = new Paciente();
        dados.setRisco(risco);
        dados.setTipo(TipoAtendimento.CLINICO);
        dados.setTriageNotes("Observacao clinica");

        return pacienteService.classificar(paciente.getId(), dados);
    }

    private Consultorio criarConsultorio(Long numero) {
        Consultorio consultorio = new Consultorio();
        consultorio.setNumero(numero);
        return consultorioRepository.save(consultorio);
    }

    private ChamadaPainel chamadaSalva(String nomePaciente, LocalDateTime criadaEm) {
        ChamadaPainel chamada = new ChamadaPainel();
        chamada.setPacienteId(1L);
        chamada.setNomePaciente(nomePaciente);
        chamada.setTicketNumber("P-0001");
        chamada.setTipo(TipoChamadaPainel.TRIAGEM);
        chamada.setDestino("Triagem");
        chamada.setCriadaEm(criadaEm);
        return chamada;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        CapturingPainelChamadasSseService capturingPainelChamadasSseService() {
            return new CapturingPainelChamadasSseService();
        }
    }

    static class CapturingPainelChamadasSseService extends PainelChamadasSseService {

        private final List<ChamadaPainelResponse> published = new ArrayList<>();

        @Override
        public void publish(ChamadaPainelResponse chamada) {
            published.add(chamada);
        }

        @Override
        public void heartbeat() {
        }

        List<ChamadaPainelResponse> published() {
            return List.copyOf(published);
        }

        void clear() {
            published.clear();
        }
    }
}
