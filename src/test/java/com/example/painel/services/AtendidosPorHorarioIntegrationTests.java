package com.example.painel.services;

import com.example.painel.dto.AtendidoPorHorarioResponse.Rechamada;
import com.example.painel.enums.EscopoIndicadores;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoChamadaPainel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=never"})
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Sql(scripts = {"file:docs/indicadores/schema.sql", "file:docs/indicadores/seed_teste_indicadores.sql"})
class AtendidosPorHorarioIntegrationTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired AtendidosPorHorarioService service;
    @Autowired IndicadorService indicadorService;
    @Autowired MockMvc mvc;
    private final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 9, 3, 23, 59, 59);

    @Test void seedGeral() {
        var r = service.listar(inicio, fim, EscopoIndicadores.GERAL);
        // Mesma população do indicador de atendidos.
        assertThat(r).hasSize((int) indicadorService.consultarIndicadores(inicio, fim).atendidos()).hasSize(14);
        assertThat(r).extracting(a -> a.chamadaConsultorioAt()).isSorted();
        assertThat(r).noneMatch(a -> a.tipo() == TipoAtendimento.SAMU);
        var primeiro = r.stream().filter(a -> a.chamadaConsultorioAt().equals(inicio.withHour(8).withMinute(5))
                && a.tipo() == TipoAtendimento.CLINICO).findFirst().orElseThrow();
        assertThat(primeiro.risco()).isEqualTo(Risco.VERMELHO);
        assertThat(primeiro.rechamadas()).containsExactly(
                new Rechamada(TipoChamadaPainel.TRIAGEM, inicio.withHour(8).withMinute(3)),
                new Rechamada(TipoChamadaPainel.CONSULTORIO, inicio.withHour(8).withMinute(7)),
                new Rechamada(TipoChamadaPainel.CONSULTORIO, inicio.withHour(8).withMinute(9)));
        // As rechamadas do painel batem com os contadores do GET /indicadores.
        assertThat(r.stream().mapToLong(a -> a.rechamadas().size()).sum())
                .isEqualTo(indicadorService.consultarIndicadores(inicio, fim).totalRechamadas());
    }

    @Test void seedSamu() {
        var r = service.listar(inicio, fim, EscopoIndicadores.SAMU);
        assertThat(r).hasSize(2).allMatch(a -> a.tipo() == TipoAtendimento.SAMU && a.risco() == Risco.LARANJA);
        assertThat(r.getFirst().rechamadas()).extracting(Rechamada::tipo)
                .containsExactly(TipoChamadaPainel.CONSULTORIO, TipoChamadaPainel.CONSULTORIO);
        assertThat(r.get(1).rechamadas()).isEmpty();
    }

    @Test void periodoVazio() {
        assertThat(service.listar(inicio.minusYears(1), fim.minusYears(1), EscopoIndicadores.GERAL)).isEmpty();
    }

    @Test void endpointNosDoisEscopos() throws Exception {
        mvc.perform(get("/indicadores/atendidos").param("inicio", inicio.toString()).param("fim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[0].nome").doesNotExist());
        mvc.perform(get("/indicadores/atendidos").param("inicio", inicio.toString()).param("fim", fim.toString())
                        .param("escopo", "SAMU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("SAMU"));
        mvc.perform(get("/indicadores/atendidos").param("inicio", fim.toString()).param("fim", inicio.toString()))
                .andExpect(status().isBadRequest());
    }
}
