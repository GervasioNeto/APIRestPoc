package com.example.painel.services;

import com.example.painel.enums.ComportamentoSuicida;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.enums.EscopoIndicadores;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static com.example.painel.Configuration.IndicadoresAcessoInterceptor.CABECALHO_SENHA;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=never"})
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Sql(scripts = {"file:docs/indicadores/schema.sql", "file:docs/indicadores/seed_teste_indicadores.sql"})
class IndicadorServiceIntegrationTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired IndicadorService service;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    // indicadores.senha em application-test.properties
    private static final String SENHA = "senha-de-teste";
    private final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 9, 3, 23, 59, 59);

    @Test void seedCompleto() {
        var r = service.consultarIndicadores(inicio, fim);
        assertThat(r.cadastrados()).isEqualTo(20);
        assertThat(r.classificados()).isEqualTo(18);
        assertThat(r.atendidos()).isEqualTo(15);
        assertThat(r.percentualClassificados()).isEqualByComparingTo("90.00");
        assertThat(r.percentualAtendidos()).isEqualByComparingTo("75.00");
        // Acolhimento: 18 amostras (inclui aguardando consulta e desistência), todas de 2 min.
        // Classificação/entrada até médico: os 15 finalizados (14 dos pares + o reclassificado).
        assertThat(r.tempoMedioAcolhimentoMinutos()).isCloseTo(2.0, within(0.000001));
        assertThat(r.tempoMedioClassificacaoAteMedicoMinutos()).isCloseTo(54.0, within(0.000001));
        assertThat(r.tempoMedioEntradaAteMedicoMinutos()).isCloseTo(59.0, within(0.000001));
        // A contagem por risco inclui também
        // a desistência já classificada (Paciente Anônimo): GERAL tem 5 laranjas, excluindo os 2 SAMU.
        // O Paciente Teste 21 (LARANJA na enfermagem, VERMELHO no médico) conta como VERMELHO.
        assertThat(r.contagensPorRisco()).containsEntry(Risco.VERMELHO, 5L)
                .containsEntry(Risco.LARANJA, 5L).containsEntry(Risco.AMARELO, 3L)
                .containsEntry(Risco.VERDE, 3L).containsEntry(Risco.AZUL, 2L)
                .containsEntry(Risco.NAO_CLASSIFICADO, 2L);
        // BE-02: a única desistência do seed é o Paciente Anônimo (Clínico), que desistiu aguardando o médico.
        assertThat(r.totalDesistencias()).isEqualTo(1);
        assertThat(r.desistenciasPorEtapa()).containsEntry(TipoStatus.AGUARDANDO_CONSULTA, 1L)
                .containsEntry(TipoStatus.AGUARDANDO_TRIAGEM, 0L).containsEntry(TipoStatus.CHAMADO, 0L);
        assertThat(r.totalRechamadas()).isEqualTo(7);
        assertThat(r.rechamadasPorRisco()).containsEntry(Risco.VERMELHO, 4L)
                .containsEntry(Risco.LARANJA, 1L).containsEntry(Risco.AMARELO, 2L)
                .containsEntry(Risco.VERDE, 0L).containsEntry(Risco.AZUL, 0L);
        assertThat(r.pacientesRechamadosPorRisco())
                .containsEntry(Risco.VERMELHO, 2L)
                .containsEntry(Risco.LARANJA, 1L)
                .containsEntry(Risco.AMARELO, 1L)
                .containsEntry(Risco.VERDE, 0L)
                .containsEntry(Risco.AZUL, 0L)
                .containsEntry(Risco.NAO_CLASSIFICADO, 0L);
        // Só o reclassificado tem a avaliação marcada; os outros 14 atendidos são anteriores à obrigatoriedade.
        assertThat(r.comportamentoSuicida()).containsEntry(ComportamentoSuicida.IDEACAO, 1L)
                .containsEntry(ComportamentoSuicida.TENTATIVA, 0L).containsEntry(ComportamentoSuicida.NENHUM, 0L);
        assertThat(r.atendidosSemAvaliacaoSuicida()).isEqualTo(14);
        var esperadas = java.util.Map.of("VERMELHO/CLINICO", 2.5, "LARANJA/CLINICO", 12.5,
                "VERMELHO/PSIQUIATRICO", 10.0 / 3, "LARANJA/PSIQUIATRICO", 10.0,
                "AMARELO/PSIQUIATRICO", 60.0, "VERDE/PSIQUIATRICO", 105.0,
                "AZUL/PSIQUIATRICO", 210.0);
        assertThat(r.porRiscoETipo()).hasSize(15);
        for (var g : r.porRiscoETipo()) {
            Double media = esperadas.get(g.risco() + "/" + g.tipo());
            if (media == null) {
                assertThat(g.tempoMedioEsperaMinutos()).isNull();
                assertThat(g.totalComProtocolo()).isZero();
                assertThat(g.percentualForaDoPrazo()).isZero();
            } else if (g.risco() == Risco.VERMELHO && g.tipo() == TipoAtendimento.PSIQUIATRICO) {
                // Par (0 e 5 min) + reclassificado (5 min): os 5 min estouram o prazo do VERMELHO (0),
                // embora coubessem nos 10 min do LARANJA da enfermagem.
                assertThat(g.tempoMedioEsperaMinutos()).isCloseTo(media, within(0.000001));
                assertThat(g.totalComProtocolo()).isEqualTo(3);
                assertThat(g.foraDoPrazo()).isEqualTo(2);
                assertThat(g.percentualForaDoPrazo()).isEqualByComparingTo("66.67");
            } else {
                assertThat(g.tempoMedioEsperaMinutos()).isCloseTo(media, within(0.000001));
                assertThat(g.totalComProtocolo()).isEqualTo(2);
                assertThat(g.foraDoPrazo()).isEqualTo(1);
                assertThat(g.percentualForaDoPrazo()).isEqualByComparingTo("50.00");
            }
        }
    }
    @ParameterizedTest
    @EnumSource(EscopoIndicadores.class)
    void periodoVazio(EscopoIndicadores escopo) {
        var r = service.consultarIndicadores(inicio.minusYears(1), fim.minusYears(1), escopo);
        assertThat(r.cadastrados()).isZero();
        assertThat(r.classificados()).isZero();
        assertThat(r.atendidos()).isZero();
        assertThat(r.percentualClassificados()).isZero();
        assertThat(r.percentualAtendidos()).isZero();
        assertThat(r.tempoMedioAcolhimentoMinutos()).isNull();
        assertThat(r.tempoMedioClassificacaoAteMedicoMinutos()).isNull();
        assertThat(r.tempoMedioEntradaAteMedicoMinutos()).isNull();
        assertThat(r.contagensPorRisco().values()).allMatch(v -> v == 0);
        assertThat(r.totalRechamadas()).isZero();
        assertThat(r.rechamadasPorRisco().values()).allMatch(v -> v == 0);
        assertThat(r.pacientesRechamadosPorRisco().values()).allMatch(v -> v == 0);
        assertThat(r.totalDesistencias()).isZero();
        assertThat(r.desistenciasPorEtapa().values()).allMatch(v -> v == 0);
        assertThat(r.porRiscoETipo()).allSatisfy(g -> {
            assertThat(g.tempoMedioEsperaMinutos()).isNull();
            assertThat(g.totalComProtocolo()).isZero();
            assertThat(g.foraDoPrazo()).isZero();
            assertThat(g.percentualForaDoPrazo()).isZero();
        });
    }
    @Test void limitesInclusivosEZeroReal() {
        var instante = inicio.withHour(8);
        var r = service.consultarIndicadores(instante, instante);
        assertThat(r.cadastrados()).isEqualTo(1);
        assertThat(r.porRiscoETipo()).filteredOn(g -> g.risco() == Risco.VERMELHO
                && g.tipo() == com.example.painel.enums.TipoAtendimento.CLINICO)
                .singleElement().satisfies(g -> assertThat(g.tempoMedioEsperaMinutos()).isZero());
        assertThat(service.consultarIndicadores(instante.plusNanos(1000), instante.plusMinutes(9)).cadastrados()).isZero();
        var fora = LocalDateTime.of(2026, 8, 25, 8, 0);
        assertThat(service.consultarIndicadores(fora, fora).cadastrados()).isEqualTo(1);
        assertThat(service.consultarIndicadores(fora, fim).cadastrados()).isEqualTo(21);
    }
    @Test void semClassificacaoNaoTemMedia() {
        var instante = LocalDateTime.of(2026, 9, 2, 15, 0);
        var r = service.consultarIndicadores(instante, instante);
        assertThat(r.cadastrados()).isEqualTo(1);
        assertThat(r.classificados()).isZero();
        assertThat(r.percentualClassificados()).isZero();
    }
    @Test void encerradoComDatasNulas() {
        jdbc.update("UPDATE paciente SET chamada_triagem_at = NULL, chamada_consultorio_at = NULL WHERE chegada_at = ?", inicio.withHour(8));
        var r = service.consultarIndicadores(inicio.withHour(8), inicio.withHour(8));
        assertThat(r.atendidos()).isEqualTo(1);
        assertThat(r.tempoMedioAcolhimentoMinutos()).isNull();
        assertThat(r.tempoMedioClassificacaoAteMedicoMinutos()).isNull();
        assertThat(r.tempoMedioEntradaAteMedicoMinutos()).isNull();
        assertThat(r.porRiscoETipo()).allSatisfy(g -> assertThat(g.tempoMedioEsperaMinutos()).isNull());
    }

    @Test void seedSamu() {
        var r = service.consultarIndicadores(inicio, fim, EscopoIndicadores.SAMU);
        assertThat(r.cadastrados()).isEqualTo(2);
        assertThat(r.classificados()).isEqualTo(2);
        assertThat(r.atendidos()).isEqualTo(2);
        assertThat(r.percentualClassificados()).isEqualByComparingTo("100.00");
        assertThat(r.percentualAtendidos()).isEqualByComparingTo("100.00");
        assertThat(r.tempoMedioAcolhimentoMinutos()).isCloseTo(4.0, within(0.000001));
        assertThat(r.tempoMedioClassificacaoAteMedicoMinutos()).isCloseTo(12.5, within(0.000001));
        assertThat(r.tempoMedioEntradaAteMedicoMinutos()).isCloseTo(17.5, within(0.000001));
        assertThat(r.contagensPorRisco()).containsEntry(Risco.LARANJA, 2L);
        assertThat(r.contagensPorRisco()).allSatisfy((cor, total) ->
                assertThat(total).isEqualTo(cor == Risco.LARANJA ? 2L : 0L));
        // BE-02: a desistência do Paciente Anônimo é Clínica e não aparece no SAMU.
        assertThat(r.totalDesistencias()).isZero();
        assertThat(r.desistenciasPorEtapa().values()).allMatch(v -> v == 0);
        assertThat(r.totalRechamadas()).isEqualTo(2);
        assertThat(r.rechamadasPorRisco()).allSatisfy((cor, total) ->
                assertThat(total).isEqualTo(cor == Risco.LARANJA ? 2L : 0L));
        assertThat(r.pacientesRechamadosPorRisco()).allSatisfy((cor, total) ->
                assertThat(total).isEqualTo(cor == Risco.LARANJA ? 1L : 0L));
        assertThat(r.porRiscoETipo()).hasSize(15).allSatisfy(g -> {
            if (g.tipo() == com.example.painel.enums.TipoAtendimento.SAMU && g.risco() == Risco.LARANJA) {
                assertThat(g.tempoMedioEsperaMinutos()).isCloseTo(12.5, within(0.000001));
                assertThat(g.totalComProtocolo()).isEqualTo(2);
                assertThat(g.foraDoPrazo()).isEqualTo(1);
                assertThat(g.percentualForaDoPrazo()).isEqualByComparingTo("50.00");
            } else {
                assertThat(g.tempoMedioEsperaMinutos()).isNull();
                assertThat(g.totalComProtocolo()).isZero();
                assertThat(g.foraDoPrazo()).isZero();
                assertThat(g.percentualForaDoPrazo()).isZero();
            }
        });
    }

    @Test void tipoNuloPertenceSomenteAoGeral() {
        var instante = LocalDateTime.of(2026, 9, 2, 15, 0);
        assertThat(service.consultarIndicadores(instante, instante, EscopoIndicadores.GERAL).cadastrados()).isEqualTo(1);
        assertThat(service.consultarIndicadores(instante, instante, EscopoIndicadores.SAMU).cadastrados()).isZero();
    }

    @Test void limitesInclusivosSamu() {
        var instante = LocalDateTime.of(2026, 9, 2, 8, 0);
        assertThat(service.consultarIndicadores(instante, instante, EscopoIndicadores.SAMU).cadastrados()).isEqualTo(1);
        assertThat(service.consultarIndicadores(instante.plusNanos(1000), instante.plusMinutes(14), EscopoIndicadores.SAMU).cadastrados()).isZero();
    }

    @Test void endpointPadraoEquivaleAGeralESamuRespeitaEscopo() throws Exception {
        var padrao = mvc.perform(get("/indicadores").header(CABECALHO_SENHA, SENHA).param("inicio", inicio.toString()).param("fim", fim.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(20))
                .andExpect(jsonPath("$.classificados").value(18))
                .andExpect(jsonPath("$.atendidos").value(15))
                .andExpect(jsonPath("$.percentualClassificados").value(90.0))
                .andExpect(jsonPath("$.percentualAtendidos").value(75.0))
                .andExpect(jsonPath("$.tempoMedioAcolhimentoMinutos").value(2.0))
                .andExpect(jsonPath("$.tempoMedioClassificacaoAteMedicoMinutos").value(54.0))
                .andExpect(jsonPath("$.tempoMedioEntradaAteMedicoMinutos").value(59.0))
                .andExpect(jsonPath("$.contagensPorRisco.VERMELHO").value(5))
                .andExpect(jsonPath("$.contagensPorRisco.LARANJA").value(5))
                .andExpect(jsonPath("$.comportamentoSuicida.IDEACAO").value(1))
                .andExpect(jsonPath("$.atendidosSemAvaliacaoSuicida").value(14))
                .andExpect(jsonPath("$.totalDesistencias").value(1))
                .andExpect(jsonPath("$.desistenciasPorEtapa.AGUARDANDO_CONSULTA").value(1))
                .andExpect(jsonPath("$.totalRechamadas").value(7))
                .andExpect(jsonPath("$.rechamadasPorRisco.VERMELHO").value(4))
                .andExpect(jsonPath("$.pacientesRechamadosPorRisco.VERMELHO").value(2))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/indicadores").header(CABECALHO_SENHA, SENHA).param("inicio", inicio.toString()).param("fim", fim.toString()).param("escopo", "GERAL"))
                .andExpect(status().isOk()).andExpect(content().json(padrao));
        mvc.perform(get("/indicadores").header(CABECALHO_SENHA, SENHA).param("inicio", inicio.toString()).param("fim", fim.toString()).param("escopo", "SAMU"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(2))
                .andExpect(jsonPath("$.classificados").value(2))
                .andExpect(jsonPath("$.atendidos").value(2))
                .andExpect(jsonPath("$.percentualClassificados").value(100.0))
                .andExpect(jsonPath("$.percentualAtendidos").value(100.0))
                .andExpect(jsonPath("$.tempoMedioAcolhimentoMinutos").value(4.0))
                .andExpect(jsonPath("$.tempoMedioClassificacaoAteMedicoMinutos").value(12.5))
                .andExpect(jsonPath("$.tempoMedioEntradaAteMedicoMinutos").value(17.5))
                .andExpect(jsonPath("$.contagensPorRisco.LARANJA").value(2))
                .andExpect(jsonPath("$.totalDesistencias").value(0))
                .andExpect(jsonPath("$.desistenciasPorEtapa.AGUARDANDO_CONSULTA").value(0))
                .andExpect(jsonPath("$.totalRechamadas").value(2))
                .andExpect(jsonPath("$.pacientesRechamadosPorRisco.LARANJA").value(1));
        mvc.perform(get("/indicadores").header(CABECALHO_SENHA, SENHA).param("inicio", inicio.toString()).param("fim", fim.toString()).param("escopo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    // INT-10: sem a senha dos indicadores (ou com a errada) o back recusa, e o navegador consegue ler a recusa.
    @Test void semSenhaOuComSenhaErradaResponde401() throws Exception {
        mvc.perform(get("/indicadores").param("inicio", inicio.toString()).param("fim", fim.toString())
                        .header("Origin", "http://localhost:8080"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(jsonPath("$.erro").value("Senha dos indicadores incorreta."));
        mvc.perform(post("/indicadores/acesso").header(CABECALHO_SENHA, "errada"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/indicadores/acesso").header(CABECALHO_SENHA, SENHA))
                .andExpect(status().isNoContent());
    }

    // O navegador pergunta antes (preflight) se pode mandar o cabeçalho da senha; sem senha, isso precisa passar.
    @Test void preflightDoNavegadorLiberaOCabecalhoDaSenha() throws Exception {
        mvc.perform(options("/indicadores")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", CABECALHO_SENHA))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase(CABECALHO_SENHA)));
    }
}
