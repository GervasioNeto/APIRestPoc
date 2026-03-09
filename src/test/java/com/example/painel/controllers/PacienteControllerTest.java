package com.example.painel.controllers;

import com.example.painel.entinty.Paciente;
import com.example.painel.enums.TipoStatus;
import com.example.painel.services.PacienteService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PacienteController.class)
public class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteService pacienteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /pacientes - Deve criar um paciente e retornar status 200")
    void testeCriarUmPaciente() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setNome("Fulano");

        when(pacienteService.criar(any(Paciente.class))).thenReturn(paciente);

        mockMvc.perform(post("/pacientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(paciente)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Fulano"));
    }

    @Test
    @DisplayName("GET /pacientes- Deve retornar a lista de pacientes com status 200")
    void testeListarTodosoOsPacientes() throws Exception {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setNome("Fulano");

        when(pacienteService.listarTodos()).thenReturn(List.of(p));

        mockMvc.perform(get("/pacientes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].nome").value("Fulano"));
    }

    @Test
    @DisplayName("GET /pacientes/aguardando-triagem - Deve retornar a lista de pacientes para triagem")
    void testeListarPacientesParaTriagem() throws Exception {
        Paciente p = new Paciente();
        p.setNome("Paciente Triagem");
        when(pacienteService.listarAguardandoTriagem()).thenReturn(List.of(p));

        mockMvc.perform(get("/pacientes/aguardando-triagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Paciente Triagem"));
    }

    @Test
    @DisplayName("PUT /pacientes/{id}/classificar - Deve atualizar classificação do paciente")
    void testeClassificarPaciente() throws Exception {
        Long id = 1L;
        Paciente dados = new Paciente();
        dados.setTriageNotes("Febre");

        Paciente resultado = new Paciente();
        resultado.setId(id);
        resultado.setTriageNotes("Febre");
        resultado.setStatus(com.example.painel.enums.TipoStatus.AGUARDANDO_CONSULTA); // Adicionado

        when(pacienteService.classificar(eq(id), any(Paciente.class))).thenReturn(resultado);

        mockMvc.perform(put("/pacientes/{id}/classificar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageNotes").value("Febre"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONSULTA")); // Validação completa
    }

    @Test
    @DisplayName("GET /pacientes/aguardando-medico - Deve retornar a fila médica")
    void testeListarFilaMedica() throws Exception {
        Paciente p1 = new Paciente();
        p1.setNome("Fulano");
        Paciente p2 = new Paciente();
        p2.setNome("Beltrano");

        when(pacienteService.listarFilaMedica()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/pacientes/aguardando-medico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Fulano")) // Corrigido para p1
                .andExpect(jsonPath("$[1].nome").value("Beltrano")); // Corrigido para p2
    }

    @Test
    @DisplayName("PUT /pacientes/{id}/chamar - Deve vincular paciente ao consultório")
    void testeChamarPaciente() throws Exception {
        Long id = 1L;
        Long consultorioId = 2L;
        Paciente p = new Paciente();
        p.setId(id);

        when(pacienteService.chamarParaConsultorio(id, consultorioId)).thenReturn(p);

        mockMvc.perform(put("/pacientes/{id}/chamar", id)
            .param("consultorioId", consultorioId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("DELETE /pacientes/{id} - Deve deletar paciente e retornar 204")
    void testeDeletarPaciente() throws Exception {
        Long id = 1L;
        doNothing().when(pacienteService).deletar(id);

        mockMvc.perform(delete("/pacientes/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /pacientes/{id}/finalizar - Deve finalizar atendimento")
    void testeFinalizarAtendimento() throws Exception {
        Long id = 1L;
        Paciente p = new Paciente();
        p.setStatus(TipoStatus.FINALIZADO);

        when(pacienteService.finalizarAtendimento(id)).thenReturn(p);

        mockMvc.perform(put("/pacientes/{id}/finalizar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    @DisplayName("PUT /pacientes/{id}/desistencia - Deve registrar desistência")
    void testeRegistrarDesistencia() throws Exception {
        Long id = 1L;
        Paciente p = new Paciente();
        p.setStatus(TipoStatus.DESISTENCIA);

        when(pacienteService.registrarDesistencia(id)).thenReturn(p);

        mockMvc.perform(put("/pacientes/{id}/desistencia", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESISTENCIA"));
    }

    @Test
    @DisplayName("PUT /pacientes/{id}/rechamar - Deve rechamar paciente")
    void testeRechamarPaciente() throws Exception {
        Long id = 1L;
        Paciente p = new Paciente();
        p.setStatus(TipoStatus.CHAMADO);

        when(pacienteService.rechamarPaciente(id)).thenReturn(p);

        mockMvc.perform(put("/pacientes/{id}/rechamar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHAMADO"));
    }

    @Test
    @DisplayName("PUT /pacientes/{id}/recolocar-fila - Deve recolocar paciente na fila")
    void testeRecolocarNaFila() throws Exception {
        Long id = 1L;
        Paciente p = new Paciente();
        p.setStatus(com.example.painel.enums.TipoStatus.AGUARDANDO_CONSULTA); // Corrigido para condizer com a lógica

        when(pacienteService.recolocarNaFila(id)).thenReturn(p);

        mockMvc.perform(put("/pacientes/{id}/recolocar-fila", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONSULTA"));
    }


}
