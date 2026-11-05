package com.example.painel.controllers;

import com.example.painel.dto.AtendidoPorHorarioResponse;
import com.example.painel.enums.EscopoIndicadores;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoChamadaPainel;
import com.example.painel.services.AtendidosPorHorarioService;
import com.example.painel.services.IndicadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AtendidosPorHorarioControllerTests {
    AtendidosPorHorarioService service;
    MockMvc mvc;
    String inicio = "2026-09-01T00:00:00";
    String fim = "2026-09-03T23:59:59";

    @BeforeEach void setup() {
        service = mock(AtendidosPorHorarioService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AtendidosPorHorarioController(service)).build();
    }

    @Test void listaSemDadosPessoais() throws Exception {
        when(service.listar(any(), any(), eq(EscopoIndicadores.GERAL))).thenReturn(List.of(
                new AtendidoPorHorarioResponse(1L, Risco.VERMELHO, TipoAtendimento.CLINICO,
                        LocalDateTime.of(2026, 9, 1, 8, 5), List.of(new AtendidoPorHorarioResponse.Rechamada(
                        TipoChamadaPainel.CONSULTORIO, LocalDateTime.of(2026, 9, 1, 8, 7))))));
        mvc.perform(get("/indicadores/atendidos").param("inicio", inicio).param("fim", fim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteId").value(1))
                .andExpect(jsonPath("$[0].risco").value("VERMELHO"))
                .andExpect(jsonPath("$[0].tipo").value("CLINICO"))
                .andExpect(jsonPath("$[0].rechamadas[0].tipo").value("CONSULTORIO"))
                .andExpect(jsonPath("$[0].nome").doesNotExist());
        verify(service).listar(LocalDateTime.parse(inicio), LocalDateTime.parse(fim), EscopoIndicadores.GERAL);
    }

    @Test void parametrosInvalidos() throws Exception {
        when(service.listar(any(), any(), any())).thenThrow(new IndicadorService.PeriodoInvalidoException());
        mvc.perform(get("/indicadores/atendidos").param("inicio", fim).param("fim", inicio))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
        mvc.perform(get("/indicadores/atendidos").param("inicio", inicio).param("fim", fim).param("escopo", "INVALIDO"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores/atendidos").param("inicio", inicio))
                .andExpect(status().isBadRequest());
    }
}
