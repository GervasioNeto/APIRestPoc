package com.example.painel.controllers;

import com.example.painel.entinty.Paciente;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.services.ChamadaPainelService;
import com.example.painel.services.PacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PacienteControllerTests {
    private PacienteRepository repository;
    private ChamadaPainelService painel;
    private MockMvc mvc;
    private Paciente paciente;

    @BeforeEach
    void setup() {
        repository = mock(PacienteRepository.class);
        painel = mock(ChamadaPainelService.class);
        PacienteService service = new PacienteService();
        ReflectionTestUtils.setField(service, "pacienteRepository", repository);
        ReflectionTestUtils.setField(service, "chamadaPainelService", painel);
        PacienteController controller = new PacienteController();
        ReflectionTestUtils.setField(controller, "pacienteService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setStatus(TipoStatus.CHAMADO);
        paciente.setChamadaTriagemAt(LocalDateTime.of(2026, 9, 29, 10, 0));
        when(repository.findById(1L)).thenReturn(Optional.of(paciente));
        when(repository.save(paciente)).thenReturn(paciente);
    }

    @Test
    void duasRechamadasRetornam200EContadoresUmEDois() throws Exception {
        for (int count = 1; count <= 2; count++) {
            mvc.perform(put("/pacientes/1/rechamar-triagem"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("CHAMADO"))
                    .andExpect(jsonPath("$.rechamadasTriagemCount").value(count))
                    .andExpect(jsonPath("$.rechamadasConsultorioCount").value(0));
        }
        verify(repository, times(2)).save(paciente);
        verify(painel, times(2)).registrarChamadaTriagem(paciente);
    }

    @Test
    void semChamadaInicialRetorna409SemSalvarOuPublicar() throws Exception {
        paciente.setChamadaTriagemAt(null);
        mvc.perform(put("/pacientes/1/rechamar-triagem"))
                .andExpect(status().isConflict());
        verify(repository, never()).save(any());
        verifyNoInteractions(painel);
    }

    @Test
    void pacienteFinalizadoRetorna409SemSalvarOuPublicar() throws Exception {
        paciente.setStatus(TipoStatus.FINALIZADO);
        mvc.perform(put("/pacientes/1/rechamar-triagem"))
                .andExpect(status().isConflict());
        verify(repository, never()).save(any());
        verifyNoInteractions(painel);
    }
}
