package com.example.painel.controllers;

import com.example.painel.entinty.Consultorio;
import com.example.painel.repository.ConsultorioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultorioController.class)
class ConsultorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /consultorios - Deve listar todos os consultórios")
    void deveListarTodosConsultorios() throws Exception {
        Consultorio c1 = new Consultorio();
        c1.setId(1L);
        c1.setNumero(101L);

        when(consultorioRepository.findAll()).thenReturn(List.of(c1));

        mockMvc.perform(get("/consultorios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(101));
    }

    @Test
    @DisplayName("GET /consultorios/{id} - Deve retornar consultório por ID")
    void deveBuscarConsultorioPorId() throws Exception {
        Long id = 1L;
        Consultorio c1 = new Consultorio();
        c1.setId(id);
        c1.setNumero(101L);

        when(consultorioRepository.findById(id)).thenReturn(Optional.of(c1));

        mockMvc.perform(get("/consultorios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(101));
    }

    @Test
    @DisplayName("POST /consultorios - Deve criar consultório")
    void deveCriarConsultorio() throws Exception {
        Consultorio c1 = new Consultorio();
        c1.setNumero(101L);

        when(consultorioRepository.save(any(Consultorio.class))).thenReturn(c1);

        mockMvc.perform(post("/consultorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(101));
    }

    @Test
    @DisplayName("PUT /consultorios/{id} - Deve atualizar consultório")
    void deveAtualizarConsultorio() throws Exception {
        Long id = 1L;
        Consultorio c1 = new Consultorio();
        c1.setNumero(102L);

        when(consultorioRepository.save(any(Consultorio.class))).thenReturn(c1);

        mockMvc.perform(put("/consultorios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(102));
    }

    @Test
    @DisplayName("DELETE /consultorios/{id} - Deve deletar consultório")
    void deveDeletarConsultorio() throws Exception {
        Long id = 1L;
        doNothing().when(consultorioRepository).deleteById(id);

        mockMvc.perform(delete("/consultorios/{id}", id))
                .andExpect(status().isOk());
    }
}
