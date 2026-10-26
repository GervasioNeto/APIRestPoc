package com.example.painel.repository;

import com.example.painel.entinty.Paciente;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.testcontainers.junit.jupiter.Testcontainers
class IndicadorRepositoryIntegrationTests {
    @org.testcontainers.junit.jupiter.Container
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres =
            new org.testcontainers.containers.PostgreSQLContainer<>("postgres:16-alpine");

    @org.springframework.test.context.DynamicPropertySource
    static void database(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @Autowired
    private IndicadorRepository indicadorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ProtocoloTempoRepository protocoloTempoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final LocalDateTime inicioJanela = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
    private final LocalDateTime fimJanela = LocalDateTime.of(2026, 9, 3, 23, 59, 59);

    @Test
    @DisplayName("Teste 1: Deve calcular contagens gerais corretamente e ignorar pacientes fora da janela")
    void deveCalcularContagensGeraisCorretamenteExcluindoForaDoPeriodo() {
        // Paciente 1: FINALIZADO, classificado, dentro da janela
        pacienteRepository.save(criarPaciente(
                "Paciente 1",
                TipoStatus.FINALIZADO,
                Risco.VERMELHO,
                TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 1, 8, 0, 0),
                LocalDateTime.of(2026, 9, 1, 8, 5, 0),
                LocalDateTime.of(2026, 9, 1, 8, 10, 0)
        ));

        // Paciente 2: AGUARDANDO_CONSULTA, classificado, dentro da janela
        pacienteRepository.save(criarPaciente(
                "Paciente 2",
                TipoStatus.AGUARDANDO_CONSULTA,
                Risco.AMARELO,
                TipoAtendimento.PSIQUIATRICO,
                LocalDateTime.of(2026, 9, 1, 9, 0, 0),
                LocalDateTime.of(2026, 9, 1, 9, 15, 0),
                null
        ));

        // Paciente 3: AGUARDANDO_TRIAGEM, não classificado (classifiedAt nulo), dentro da janela
        pacienteRepository.save(criarPaciente(
                "Paciente 3",
                TipoStatus.AGUARDANDO_TRIAGEM,
                Risco.NAO_CLASSIFICADO,
                null,
                LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                null,
                null
        ));

        // Paciente 4: FINALIZADO, classificado, mas fora da janela de datas (25/08)
        pacienteRepository.save(criarPaciente(
                "Paciente 4 - Fora da Janela",
                TipoStatus.FINALIZADO,
                Risco.VERMELHO,
                TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 8, 25, 8, 0, 0),
                LocalDateTime.of(2026, 8, 25, 8, 5, 0),
                LocalDateTime.of(2026, 8, 25, 8, 10, 0)
        ));

        entityManager.flush();

        long cadastrados = indicadorRepository.countCadastrados(inicioJanela, fimJanela, false);
        long classificados = indicadorRepository.countClassificados(inicioJanela, fimJanela, false);
        long atendidos = indicadorRepository.countAtendidos(inicioJanela, fimJanela, false);

        // Esperado: 3 cadastrados na janela, 2 classificados, 1 atendido
        // O paciente 4 (de 25/08) não deve ser contabilizado
        assertThat(cadastrados).isEqualTo(3L);
        assertThat(classificados).isEqualTo(2L);
        assertThat(atendidos).isEqualTo(1L);
    }

    @Test
    @DisplayName("Teste 2: Deve calcular tempo médio de espera por risco e tipo via AVG nativo")
    void deveCalcularTempoMedioDeEsperaPorRiscoETipo() {
        // Paciente 1: LARANJA + CLINICO, classifiedAt: 09:05, chamadaConsultorioAt: 09:10 -> 5 minutos de espera
        pacienteRepository.save(criarPaciente(
                "Paciente 1",
                TipoStatus.FINALIZADO,
                Risco.LARANJA,
                TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 1, 9, 0, 0),
                LocalDateTime.of(2026, 9, 1, 9, 5, 0),
                LocalDateTime.of(2026, 9, 1, 9, 10, 0)
        ));

        // Paciente 2: LARANJA + CLINICO, classifiedAt: 09:20, chamadaConsultorioAt: 09:40 -> 20 minutos de espera
        pacienteRepository.save(criarPaciente(
                "Paciente 2",
                TipoStatus.FINALIZADO,
                Risco.LARANJA,
                TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 1, 9, 15, 0),
                LocalDateTime.of(2026, 9, 1, 9, 20, 0),
                LocalDateTime.of(2026, 9, 1, 9, 40, 0)
        ));

        entityManager.flush();

        List<IndicadorRepository.TempoMedioPorRiscoTipo> resultados =
                indicadorRepository.tempoMedioEsperaPorRiscoETipo(inicioJanela, fimJanela, false);

        assertThat(resultados).hasSize(1);
        IndicadorRepository.TempoMedioPorRiscoTipo item = resultados.get(0);
        assertThat(item.getRisco()).isEqualTo("LARANJA");
        assertThat(item.getTipo()).isEqualTo("CLINICO");

        // Média esperada: (5 + 20) / 2 = 12.5 minutos
        assertThat(item.getTempoMedioMinutos()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("Teste 3: Deve calcular contagem de pacientes fora do protocolo e validar percentual de 50%")
    void deveCalcularPacientesForaDoProtocoloEPercentual() {
        // Cadastra protocolo de tempo: LARANJA + CLINICO = limite de 10 minutos
        ProtocoloTempo protocolo = new ProtocoloTempo();
        protocolo.setRisco(Risco.LARANJA);
        protocolo.setTipo(TipoAtendimento.CLINICO);
        protocolo.setTempoMaximoMinutos(10);
        protocoloTempoRepository.save(protocolo);

        // Paciente A: LARANJA + CLINICO, espera de 5 minutos (dentro do prazo de 10 min)
        pacienteRepository.save(criarPaciente(
                "Paciente A",
                TipoStatus.FINALIZADO,
                Risco.LARANJA,
                TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 1, 9, 0, 0),
                LocalDateTime.of(2026, 9, 1, 9, 5, 0),
                LocalDateTime.of(2026, 9, 1, 9, 10, 0)
        ));

        // Paciente B: LARANJA + CLINICO, espera de 20 minutos (fora do prazo de 10 min)
        pacienteRepository.save(criarPaciente(
                "Paciente B",
                TipoStatus.FINALIZADO,
                Risco.LARANJA,
                TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 1, 9, 15, 0),
                LocalDateTime.of(2026, 9, 1, 9, 20, 0),
                LocalDateTime.of(2026, 9, 1, 9, 40, 0)
        ));

        entityManager.flush();

        List<IndicadorRepository.ForaDoProtocoloPorRiscoTipo> resultados =
                indicadorRepository.contarForaDoProtocoloPorRiscoETipo(inicioJanela, fimJanela, false);

        assertThat(resultados).hasSize(1);
        IndicadorRepository.ForaDoProtocoloPorRiscoTipo item = resultados.get(0);
        assertThat(item.getRisco()).isEqualTo("LARANJA");
        assertThat(item.getTipo()).isEqualTo("CLINICO");
        assertThat(item.getTotal()).isEqualTo(2L);
        assertThat(item.getForaDoPrazo()).isEqualTo(1L);

        // Validação da regra de percentual: fora / total * 100
        double percentual = (item.getForaDoPrazo().doubleValue() / item.getTotal()) * 100.0;
        assertThat(percentual).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Teste 4: Deve calcular os tempos médios 1-3 sem filtrar status, ignorando datas nulas e respeitando o escopo")
    void deveCalcularTemposMediosGeraisPorEscopo() {
        // Paciente A: FINALIZADO -> acolhimento 4, classificação->médico 20, entrada->médico 30
        Paciente a = criarPaciente("Paciente A", TipoStatus.FINALIZADO, Risco.LARANJA, TipoAtendimento.CLINICO,
                LocalDateTime.of(2026, 9, 1, 9, 0, 0),
                LocalDateTime.of(2026, 9, 1, 9, 10, 0),
                LocalDateTime.of(2026, 9, 1, 9, 30, 0));
        a.setChamadaTriagemAt(LocalDateTime.of(2026, 9, 1, 9, 4, 0));
        pacienteRepository.save(a);

        // Paciente B: chamado ao consultório e não compareceu (DESISTENCIA) -> 2 / 40 / 45, entra nas três médias
        Paciente b = criarPaciente("Paciente B", TipoStatus.DESISTENCIA, Risco.AMARELO, TipoAtendimento.PSIQUIATRICO,
                LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                LocalDateTime.of(2026, 9, 1, 10, 5, 0),
                LocalDateTime.of(2026, 9, 1, 10, 45, 0));
        b.setChamadaTriagemAt(LocalDateTime.of(2026, 9, 1, 10, 2, 0));
        pacienteRepository.save(b);

        // Paciente C: ainda aguardando consulta -> só acolhimento (6); sem chamada ao consultório fica fora de 2 e 3
        Paciente c = criarPaciente("Paciente C", TipoStatus.AGUARDANDO_CONSULTA, Risco.VERDE, TipoAtendimento.PSIQUIATRICO,
                LocalDateTime.of(2026, 9, 1, 11, 0, 0),
                LocalDateTime.of(2026, 9, 1, 11, 10, 0),
                null);
        c.setChamadaTriagemAt(LocalDateTime.of(2026, 9, 1, 11, 6, 0));
        pacienteRepository.save(c);

        // Paciente D: SAMU -> 30 / 60 / 95, só no escopo SAMU
        Paciente d = criarPaciente("Paciente D - SAMU", TipoStatus.FINALIZADO, Risco.LARANJA, TipoAtendimento.SAMU,
                LocalDateTime.of(2026, 9, 1, 13, 0, 0),
                LocalDateTime.of(2026, 9, 1, 13, 35, 0),
                LocalDateTime.of(2026, 9, 1, 14, 35, 0));
        d.setChamadaTriagemAt(LocalDateTime.of(2026, 9, 1, 13, 30, 0));
        pacienteRepository.save(d);

        entityManager.flush();

        // GERAL: acolhimento (4 + 2 + 6) / 3 = 4.0; classificação->médico (20 + 40) / 2 = 30.0; entrada->médico (30 + 45) / 2 = 37.5
        IndicadorRepository.TemposMedios geral = indicadorRepository.temposMedios(inicioJanela, fimJanela, false);
        assertThat(geral.getAcolhimentoMinutos()).isEqualTo(4.0);
        assertThat(geral.getClassificacaoAteMedicoMinutos()).isEqualTo(30.0);
        assertThat(geral.getEntradaAteMedicoMinutos()).isEqualTo(37.5);

        IndicadorRepository.TemposMedios samu = indicadorRepository.temposMedios(inicioJanela, fimJanela, true);
        assertThat(samu.getAcolhimentoMinutos()).isEqualTo(30.0);
        assertThat(samu.getClassificacaoAteMedicoMinutos()).isEqualTo(60.0);
        assertThat(samu.getEntradaAteMedicoMinutos()).isEqualTo(95.0);
    }

    @Test
    @DisplayName("Teste 5: Sem amostras no período, os tempos médios 1-3 vêm nulos")
    void temposMediosSemAmostrasSaoNulos() {
        IndicadorRepository.TemposMedios tempos = indicadorRepository.temposMedios(inicioJanela, fimJanela, false);

        assertThat(tempos).isNotNull();
        assertThat(tempos.getAcolhimentoMinutos()).isNull();
        assertThat(tempos.getClassificacaoAteMedicoMinutos()).isNull();
        assertThat(tempos.getEntradaAteMedicoMinutos()).isNull();
    }

    @Test
    @DisplayName("IND-08: soma rechamadas e conta pacientes distintos por risco, período e escopo")
    void deveCalcularRechamadasPorRiscoEPorEscopo() {
        Paciente geral = criarPaciente("Geral", TipoStatus.FINALIZADO, Risco.VERMELHO,
                TipoAtendimento.CLINICO, inicioJanela.plusHours(1), inicioJanela.plusHours(1), null);
        geral.setRechamadasTriagemCount(2);
        geral.setRechamadasConsultorioCount(3);
        pacienteRepository.save(geral);

        Paciente samu = criarPaciente("SAMU", TipoStatus.FINALIZADO, Risco.LARANJA,
                TipoAtendimento.SAMU, inicioJanela.plusHours(2), inicioJanela.plusHours(2), null);
        samu.setRechamadasTriagemCount(null);
        samu.setRechamadasConsultorioCount(4);
        pacienteRepository.save(samu);

        Paciente semRechamada = criarPaciente("Sem rechamada", TipoStatus.FINALIZADO, Risco.AMARELO,
                TipoAtendimento.CLINICO, inicioJanela.plusHours(3), inicioJanela.plusHours(3), null);
        pacienteRepository.save(semRechamada);

        Paciente fora = criarPaciente("Fora", TipoStatus.FINALIZADO, Risco.AZUL,
                TipoAtendimento.CLINICO, inicioJanela.minusDays(1), inicioJanela.minusDays(1), null);
        fora.setRechamadasTriagemCount(10);
        pacienteRepository.save(fora);
        entityManager.flush();

        assertThat(indicadorRepository.totalRechamadas(inicioJanela, fimJanela, false)).isEqualTo(5);
        assertThat(indicadorRepository.contarPacientesRechamadosPorRisco(inicioJanela, fimJanela, false))
                .singleElement().satisfies(item -> {
                    assertThat(item.getRisco()).isEqualTo(Risco.VERMELHO);
                    assertThat(item.getTotal()).isEqualTo(1);
                });
        assertThat(indicadorRepository.totalRechamadas(inicioJanela, fimJanela, true)).isEqualTo(4);
        assertThat(indicadorRepository.contarPacientesRechamadosPorRisco(inicioJanela, fimJanela, true))
                .singleElement().satisfies(item -> {
                    assertThat(item.getRisco()).isEqualTo(Risco.LARANJA);
                    assertThat(item.getTotal()).isEqualTo(1);
                });
    }

    private Paciente criarPaciente(String nome,
                                   TipoStatus status,
                                   Risco risco,
                                   TipoAtendimento tipo,
                                   LocalDateTime chegadaAt,
                                   LocalDateTime classifiedAt,
                                   LocalDateTime chamadaConsultorioAt) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setCpf("000.000.000-00");
        p.setStatus(status);
        p.setRisco(risco);
        p.setTipo(tipo);
        p.setChegadaAt(chegadaAt);
        p.setClassifiedAt(classifiedAt);
        p.setChamadaConsultorioAt(chamadaConsultorioAt);
        return p;
    }
}
