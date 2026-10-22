package com.example.painel.repository;

import com.example.painel.entinty.ChamadaPainel;
import com.example.painel.enums.TipoChamadaPainel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChamadaPainelRepositoryIntegrationTests {

    @Autowired
    private ChamadaPainelRepository chamadaPainelRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Deve retornar as chamadas do paciente em ordem cronológica, ignorando outros pacientes")
    void deveRetornarChamadasDoPacienteEmOrdemCronologica() {
        Long pacienteId = 900001L;
        Long outroPacienteId = 900002L;

        // Salvas fora de ordem para garantir que a ordenação vem da query
        chamadaPainelRepository.save(criarChamada(pacienteId, TipoChamadaPainel.CONSULTORIO, "Consultorio 2",
                LocalDateTime.of(2026, 9, 1, 10, 30, 0)));
        chamadaPainelRepository.save(criarChamada(pacienteId, TipoChamadaPainel.TRIAGEM, "Triagem",
                LocalDateTime.of(2026, 9, 1, 9, 0, 0)));
        chamadaPainelRepository.save(criarChamada(pacienteId, TipoChamadaPainel.CONSULTORIO, "Consultorio 2",
                LocalDateTime.of(2026, 9, 1, 10, 40, 0)));
        chamadaPainelRepository.save(criarChamada(pacienteId, TipoChamadaPainel.CONSULTORIO, "Consultorio 2",
                LocalDateTime.of(2026, 9, 1, 10, 20, 0)));
        chamadaPainelRepository.save(criarChamada(outroPacienteId, TipoChamadaPainel.TRIAGEM, "Triagem",
                LocalDateTime.of(2026, 9, 1, 9, 30, 0)));

        entityManager.flush();
        entityManager.clear();

        List<ChamadaPainel> chamadas = chamadaPainelRepository.findByPacienteIdOrderByCriadaEmAsc(pacienteId);

        assertThat(chamadas).hasSize(4);
        assertThat(chamadas).allMatch(chamada -> chamada.getPacienteId().equals(pacienteId));
        assertThat(chamadas).extracting(ChamadaPainel::getTipo).containsExactly(
                TipoChamadaPainel.TRIAGEM,
                TipoChamadaPainel.CONSULTORIO,
                TipoChamadaPainel.CONSULTORIO,
                TipoChamadaPainel.CONSULTORIO);
        assertThat(chamadas).extracting(ChamadaPainel::getCriadaEm).containsExactly(
                LocalDateTime.of(2026, 9, 1, 9, 0, 0),
                LocalDateTime.of(2026, 9, 1, 10, 20, 0),
                LocalDateTime.of(2026, 9, 1, 10, 30, 0),
                LocalDateTime.of(2026, 9, 1, 10, 40, 0));
    }

    @Test
    @DisplayName("Deve retornar lista vazia para paciente sem chamadas")
    void deveRetornarListaVaziaParaPacienteSemChamadas() {
        assertThat(chamadaPainelRepository.findByPacienteIdOrderByCriadaEmAsc(900003L)).isEmpty();
    }

    private ChamadaPainel criarChamada(Long pacienteId, TipoChamadaPainel tipo, String destino,
                                       LocalDateTime criadaEm) {
        ChamadaPainel chamada = new ChamadaPainel();
        chamada.setPacienteId(pacienteId);
        chamada.setNomePaciente("Paciente " + pacienteId);
        chamada.setTicketNumber("P-0001");
        chamada.setTipo(tipo);
        chamada.setDestino(destino);
        chamada.setCriadaEm(criadaEm);
        return chamada;
    }
}
