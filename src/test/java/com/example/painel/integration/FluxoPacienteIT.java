package com.example.painel.integration;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FluxoPacienteIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @BeforeEach
    void setup() {
        pacienteRepository.deleteAll();
        consultorioRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve realizar o ciclo completo de atendimento do paciente")
    void deveRealizarCicloCompletoAtendimento() throws Exception {
        // 1. Criar Consultório
        Consultorio consultorio = new Consultorio();
        consultorio.setNumero(101L);
        consultorio = consultorioRepository.save(consultorio);

        // 2. Chegada do Paciente (POST /pacientes)
        Paciente novoPaciente = new Paciente();
        novoPaciente.setNome("João da Silva");
        novoPaciente.setCpf("12345678900");

        MvcResult createResult = mockMvc.perform(post("/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novoPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_TRIAGEM"))
                .andReturn();

        Paciente pacienteCriado = objectMapper.readValue(createResult.getResponse().getContentAsString(), Paciente.class);
        Long pacienteId = pacienteCriado.getId();

        // 3. Triagem e Classificação (PUT /pacientes/{id}/classificar)
        Paciente dadosClassificacao = new Paciente();
        dadosClassificacao.setRisco(Risco.AMARELO);
        dadosClassificacao.setTipo(TipoAtendimento.CLINICO);
        dadosClassificacao.setTriageNotes("Dor moderada");

        mockMvc.perform(put("/pacientes/{id}/classificar", pacienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dadosClassificacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONSULTA"))
                .andExpect(jsonPath("$.risco").value("AMARELO"));

        // 4. Verificar se está na fila médica (GET /pacientes/aguardando-medico)
        mockMvc.perform(get("/pacientes/aguardando-medico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pacienteId));

        // 5. Chamar para Consultório (PUT /pacientes/{id}/chamar)
        mockMvc.perform(put("/pacientes/{id}/chamar", pacienteId)
                        .param("consultorioId", consultorio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHAMADO"))
                .andExpect(jsonPath("$.consultorio.numero").value(101));

        // 6. Finalizar Atendimento (PUT /pacientes/{id}/finalizar)
        mockMvc.perform(put("/pacientes/{id}/finalizar", pacienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));

        // Verificação final no banco
        Paciente pacienteFinal = pacienteRepository.findById(pacienteId).orElseThrow();
        assertThat(pacienteFinal.getStatus()).isEqualTo(TipoStatus.FINALIZADO);
    }
}
