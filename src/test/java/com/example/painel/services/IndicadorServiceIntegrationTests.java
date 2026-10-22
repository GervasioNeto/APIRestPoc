package com.example.painel.services;

import com.example.painel.enums.Risco;
import com.example.painel.enums.EscopoIndicadores;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 9, 3, 23, 59, 59);

    @Test void seedCompleto() {
        var r = service.consultarIndicadores(inicio, fim);
        assertThat(r.cadastrados()).isEqualTo(19);
        assertThat(r.classificados()).isEqualTo(17);
        assertThat(r.atendidos()).isEqualTo(14);
        assertThat(r.percentualClassificados()).isEqualByComparingTo("89.47");
        assertThat(r.percentualAtendidos()).isEqualByComparingTo("73.68");
        // A contagem por risco inclui também
        // a desistência já classificada (Paciente Anônimo): GERAL tem 5 laranjas, excluindo os 2 SAMU.
        assertThat(r.contagensPorRisco()).containsEntry(Risco.VERMELHO, 4L)
                .containsEntry(Risco.LARANJA, 5L).containsEntry(Risco.AMARELO, 3L)
                .containsEntry(Risco.VERDE, 3L).containsEntry(Risco.AZUL, 2L)
                .containsEntry(Risco.NAO_CLASSIFICADO, 2L);
        var esperadas = java.util.Map.of("VERMELHO/CLINICO", 2.5, "LARANJA/CLINICO", 12.5,
                "VERMELHO/PSIQUIATRICO", 2.5, "LARANJA/PSIQUIATRICO", 10.0,
                "AMARELO/PSIQUIATRICO", 60.0, "VERDE/PSIQUIATRICO", 105.0,
                "AZUL/PSIQUIATRICO", 210.0);
        assertThat(r.porRiscoETipo()).hasSize(15);
        for (var g : r.porRiscoETipo()) {
            Double media = esperadas.get(g.risco() + "/" + g.tipo());
            if (media == null) {
                assertThat(g.tempoMedioEsperaMinutos()).isNull();
                assertThat(g.totalComProtocolo()).isZero();
                assertThat(g.percentualForaDoPrazo()).isZero();
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
        assertThat(r.contagensPorRisco().values()).allMatch(v -> v == 0);
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
        assertThat(service.consultarIndicadores(fora, fim).cadastrados()).isEqualTo(20);
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
        assertThat(r.porRiscoETipo()).allSatisfy(g -> assertThat(g.tempoMedioEsperaMinutos()).isNull());
    }

    @Test void seedSamu() {
        var r = service.consultarIndicadores(inicio, fim, EscopoIndicadores.SAMU);
        assertThat(r.cadastrados()).isEqualTo(2);
        assertThat(r.classificados()).isEqualTo(2);
        assertThat(r.atendidos()).isEqualTo(2);
        assertThat(r.percentualClassificados()).isEqualByComparingTo("100.00");
        assertThat(r.percentualAtendidos()).isEqualByComparingTo("100.00");
        assertThat(r.contagensPorRisco()).containsEntry(Risco.LARANJA, 2L);
        assertThat(r.contagensPorRisco()).allSatisfy((cor, total) ->
                assertThat(total).isEqualTo(cor == Risco.LARANJA ? 2L : 0L));
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
        var padrao = mvc.perform(get("/indicadores").param("inicio", inicio.toString()).param("fim", fim.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(19))
                .andExpect(jsonPath("$.classificados").value(17))
                .andExpect(jsonPath("$.atendidos").value(14))
                .andExpect(jsonPath("$.percentualClassificados").value(89.47))
                .andExpect(jsonPath("$.percentualAtendidos").value(73.68))
                .andExpect(jsonPath("$.contagensPorRisco.LARANJA").value(5))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/indicadores").param("inicio", inicio.toString()).param("fim", fim.toString()).param("escopo", "GERAL"))
                .andExpect(status().isOk()).andExpect(content().json(padrao));
        mvc.perform(get("/indicadores").param("inicio", inicio.toString()).param("fim", fim.toString()).param("escopo", "SAMU"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(2))
                .andExpect(jsonPath("$.classificados").value(2))
                .andExpect(jsonPath("$.atendidos").value(2))
                .andExpect(jsonPath("$.percentualClassificados").value(100.0))
                .andExpect(jsonPath("$.percentualAtendidos").value(100.0))
                .andExpect(jsonPath("$.contagensPorRisco.LARANJA").value(2));
        mvc.perform(get("/indicadores").param("inicio", inicio.toString()).param("fim", fim.toString()).param("escopo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }
}
