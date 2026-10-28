package com.example.painel.controllers;

import com.example.painel.dto.ProtocoloTempoResponse;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.services.ProtocoloTempoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProtocoloTempoControllerTests {
    ProtocoloTempoService service;
    MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(ProtocoloTempoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProtocoloTempoController(service)).build();
    }

    @Test void listaProtocolos() throws Exception {
        when(service.listar()).thenReturn(List.of(
                new ProtocoloTempoResponse(Risco.VERMELHO, TipoAtendimento.CLINICO, 0),
                new ProtocoloTempoResponse(Risco.LARANJA, TipoAtendimento.SAMU, 10)));

        mvc.perform(get("/protocolos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].risco").value("VERMELHO"))
                .andExpect(jsonPath("$[0].tipo").value("CLINICO"))
                .andExpect(jsonPath("$[0].tempoMaximoMinutos").value(0))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[1].risco").value("LARANJA"))
                .andExpect(jsonPath("$[1].tipo").value("SAMU"))
                .andExpect(jsonPath("$[1].tempoMaximoMinutos").value(10));
    }

    @Test void semProtocoloCadastradoRetornaListaVazia() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mvc.perform(get("/protocolos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
