package com.example.painel.controllers;

import com.example.painel.dto.IndicadoresResponse;
import com.example.painel.services.IndicadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IndicadorControllerTests {
    IndicadorService service;
    MockMvc mvc;
    String inicio = "2026-09-01T00:00:00";
    String fim = "2026-09-09T23:59:59";
    @BeforeEach void setup() {
        service = mock(IndicadorService.class);
        mvc = MockMvcBuilders.standaloneSetup(new IndicadorController(service)).build();
    }
    @Test void consultaValida() throws Exception {
        when(service.consultarIndicadores(any(), any())).thenReturn(new IndicadoresResponse(
                4, 3, 2, new BigDecimal("75.00"), new BigDecimal("50.00"),
                Map.of(com.example.painel.enums.Risco.LARANJA, 3L), List.of(
                        new IndicadoresResponse.Detalhamento(com.example.painel.enums.Risco.LARANJA,
                                com.example.painel.enums.TipoAtendimento.CLINICO, 12.5,
                                2, 1, new BigDecimal("50.00")))));
        mvc.perform(get("/indicadores").param("inicio", inicio).param("fim", fim))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(4))
                .andExpect(jsonPath("$.percentualClassificados").value(75.0))
                .andExpect(jsonPath("$.contagensPorRisco.LARANJA").value(3))
                .andExpect(jsonPath("$.porRiscoETipo[0].risco").value("LARANJA"))
                .andExpect(jsonPath("$.porRiscoETipo[0].tipo").value("CLINICO"))
                .andExpect(jsonPath("$.porRiscoETipo[0].tempoMedioEsperaMinutos").value(12.5))
                .andExpect(jsonPath("$.porRiscoETipo[0].percentualForaDoPrazo").value(50.0));
        verify(service).consultarIndicadores(java.time.LocalDateTime.parse(inicio), java.time.LocalDateTime.parse(fim));
    }
    @Test void parametrosInvalidos() throws Exception {
        mvc.perform(get("/indicadores")).andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores").param("inicio", inicio)).andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores").param("fim", fim)).andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores").param("inicio", "invalido").param("fim", fim)).andExpect(status().isBadRequest());
        mvc.perform(get("/indicadores").param("inicio", inicio).param("fim", "2026-02-30T00:00:00")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void periodoInvertido() throws Exception {
        when(service.consultarIndicadores(any(), any())).thenThrow(new IndicadorService.PeriodoInvalidoException());
        mvc.perform(get("/indicadores").param("inicio", fim).param("fim", inicio)).andExpect(status().isBadRequest());
    }
    @Test void periodoVazioEDatasIguais() throws Exception {
        when(service.consultarIndicadores(any(), any())).thenReturn(new IndicadoresResponse(
                0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), List.of(
                        new IndicadoresResponse.Detalhamento(com.example.painel.enums.Risco.VERMELHO,
                                com.example.painel.enums.TipoAtendimento.CLINICO, null,
                                0, 0, BigDecimal.ZERO))));
        mvc.perform(get("/indicadores").param("inicio", inicio).param("fim", inicio))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(0))
                .andExpect(jsonPath("$.percentualAtendidos").value(0))
                .andExpect(jsonPath("$.tempoMedioAteClassificacaoMinutos").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"tempoMedioEsperaMinutos\":null")));
    }
    @Test void falhaDeBancoNaoVira400() {
        when(service.consultarIndicadores(any(), any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("indisponível"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                mvc.perform(get("/indicadores").param("inicio", inicio).param("fim", fim)))
                .hasCauseInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
    }
}
