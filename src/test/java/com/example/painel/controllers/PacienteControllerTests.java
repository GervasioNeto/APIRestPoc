package com.example.painel.controllers;

import com.example.painel.dto.HistoricoChamadaResponse;
import com.example.painel.entinty.Paciente;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.ProtocoloTempoRepository;
import com.example.painel.services.ChamadaPainelService;
import com.example.painel.services.PacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PacienteControllerTests {
    private PacienteRepository repository;
    private ChamadaPainelService painel;
    private ProtocoloTempoRepository protocoloTempoRepository;
    private MockMvc mvc;
    private Paciente paciente;

    @BeforeEach
    void setup() {
        repository = mock(PacienteRepository.class);
        painel = mock(ChamadaPainelService.class);
        protocoloTempoRepository = mock(ProtocoloTempoRepository.class);
        PacienteService service = new PacienteService();
        ReflectionTestUtils.setField(service, "pacienteRepository", repository);
        ReflectionTestUtils.setField(service, "chamadaPainelService", painel);
        ReflectionTestUtils.setField(service, "protocoloTempoRepository", protocoloTempoRepository);
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

    @Test
    void historicoDeChamadasRetornaTriagemEConsultorioEmOrdemCronologica() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);
        when(painel.listarHistoricoDoPaciente(1L)).thenReturn(List.of(
                new HistoricoChamadaResponse("TRIAGEM", "Triagem", LocalDateTime.of(2026, 9, 29, 10, 0)),
                new HistoricoChamadaResponse("CONSULTORIO", "Consultorio 2", LocalDateTime.of(2026, 9, 29, 10, 20)),
                new HistoricoChamadaResponse("CONSULTORIO", "Consultorio 2", LocalDateTime.of(2026, 9, 29, 10, 25)),
                new HistoricoChamadaResponse("CONSULTORIO", "Consultorio 2", LocalDateTime.of(2026, 9, 29, 10, 30))));

        mvc.perform(get("/pacientes/1/chamadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].tipo").value("TRIAGEM"))
                .andExpect(jsonPath("$[0].destino").value("Triagem"))
                .andExpect(jsonPath("$[0].criadaEm").exists())
                .andExpect(jsonPath("$[1].tipo").value("CONSULTORIO"))
                .andExpect(jsonPath("$[3].destino").value("Consultorio 2"))
                .andExpect(jsonPath("$[0].nomePaciente").doesNotExist());
        verify(painel).listarHistoricoDoPaciente(1L);
    }

    @Test
    void historicoDePacienteInexistenteRetorna404() throws Exception {
        when(repository.existsById(99L)).thenReturn(false);

        mvc.perform(get("/pacientes/99/chamadas"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(painel);
    }

    @Test
    void filaMedicaRetornaPrazoAtendimentoOuNullSemProtocolo() throws Exception {
        Paciente laranja = new Paciente();
        laranja.setId(2L);
        laranja.setRisco(Risco.LARANJA);
        laranja.setTipo(TipoAtendimento.CLINICO);
        laranja.setClassifiedAt(LocalDateTime.of(2026, 9, 29, 10, 0));
        Paciente verde = new Paciente();
        verde.setId(3L);
        verde.setRisco(Risco.VERDE);
        verde.setTipo(TipoAtendimento.CLINICO);
        verde.setClassifiedAt(LocalDateTime.of(2026, 9, 29, 10, 0));
        ProtocoloTempo protocolo = new ProtocoloTempo();
        protocolo.setRisco(Risco.LARANJA);
        protocolo.setTipo(TipoAtendimento.CLINICO);
        protocolo.setTempoMaximoMinutos(10);
        when(repository.buscarFilaDeEsperaOrdenada()).thenReturn(List.of(laranja, verde));
        when(protocoloTempoRepository.findAll()).thenReturn(List.of(protocolo));

        mvc.perform(get("/pacientes/aguardando-medico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                // standaloneSetup usa o ObjectMapper padrão (LocalDateTime como array); na aplicação sai em ISO
                .andExpect(jsonPath("$[0].prazoAtendimentoAt").value(org.hamcrest.Matchers.contains(2026, 9, 29, 10, 10)))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1]").value(org.hamcrest.Matchers.hasKey("prazoAtendimentoAt")))
                .andExpect(jsonPath("$[1].prazoAtendimentoAt").value(org.hamcrest.Matchers.nullValue()));
        verify(protocoloTempoRepository, times(1)).findAll();
    }
}
