package com.example.painel.services;

import com.example.painel.enums.Risco;
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
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 9, 3, 23, 59, 59);

    @Test void seedCompleto() {
        var r = service.consultarIndicadores(inicio, fim);
        assertThat(r.cadastrados()).isEqualTo(21);
        assertThat(r.classificados()).isEqualTo(19);
        assertThat(r.atendidos()).isEqualTo(16);
        assertThat(r.percentualClassificados()).isEqualByComparingTo("90.48");
        assertThat(r.percentualAtendidos()).isEqualByComparingTo("76.19");
        // A contagem por risco inclui também
        // a desistência já classificada (Paciente Anônimo): o seed soma 7.
        assertThat(r.contagensPorRisco()).containsEntry(Risco.VERMELHO, 4L)
                .containsEntry(Risco.LARANJA, 7L).containsEntry(Risco.AMARELO, 3L)
                .containsEntry(Risco.VERDE, 3L).containsEntry(Risco.AZUL, 2L)
                .containsEntry(Risco.NAO_CLASSIFICADO, 2L);
        var esperadas = java.util.Map.of("VERMELHO/CLINICO", 2.5, "LARANJA/CLINICO", 12.5,
                "VERMELHO/PSIQUIATRICO", 2.5, "LARANJA/PSIQUIATRICO", 10.0,
                "AMARELO/PSIQUIATRICO", 60.0, "VERDE/PSIQUIATRICO", 105.0,
                "AZUL/PSIQUIATRICO", 210.0, "LARANJA/SAMU", 12.5);
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
    @Test void periodoVazio() {
        var r = service.consultarIndicadores(inicio.minusYears(1), fim.minusYears(1));
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
        assertThat(service.consultarIndicadores(fora, fim).cadastrados()).isEqualTo(22);
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
}
