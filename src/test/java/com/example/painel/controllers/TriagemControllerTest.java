package com.example.painel.controllers;

import com.example.painel.entinty.Triagem;
import com.example.painel.repository.TriagemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TriagemController.class)
class TriagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriagemRepository triagemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /triagens - Deve listar todas as triagens")
    void deveListarTodasTriagens() throws Exception {
        Triagem t1 = new Triagem();
        t1.setId(1L);
        t1.setNumero(101L);

        when(triagemRepository.findAll()).thenReturn(List.of(t1));

        mockMvc.perform(get("/triagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(101));
    }

    @Test
    @DisplayName("POST /triagens - Deve criar triagem")
    void deveCriarTriagem() throws Exception {
        Triagem t1 = new Triagem();
        t1.setNumero(101L);

        when(triagemRepository.save(any(Triagem.class))).thenReturn(t1);

        mockMvc.perform(post("/triagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(101));
    }

    @Test
    @DisplayName("PUT /triagens/{id} - Deve atualizar triagem")
    void deveAtualizarTriagem() throws Exception {
        Long id = 1L;
        Triagem t1 = new Triagem();
        t1.setNumero(102L);

        when(triagemRepository.save(any(Triagem.class))).thenReturn(t1);

        mockMvc.perform(put("/triagens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(102));
    }

    @Test
    @DisplayName("DELETE /triagens/{id} - Deve deletar triagem")
    void deveDeletarTriagem() throws Exception {
        Long id = 1L;
        doNothing().when(triagemRepository).deleteById(id);

        mockMvc.perform(delete("/triagens/{id}", id))
                .andExpect(status().isOk());
    }
}
