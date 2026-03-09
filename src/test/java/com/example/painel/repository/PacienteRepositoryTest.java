package com.example.painel.repository;

import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PacienteRepositoryTest
{
    @Autowired
    private PacienteRepository repository;

    @Test
    @DisplayName("Deve retornar a fila de pacientes ordenada com risco.")
    void testeBuscarFilaDeEsperaOrdenada() {
        Paciente verde = criarPaciente("Verde", Risco.VERDE);
        Paciente vermelho = criarPaciente("Vermelho", Risco.VERMELHO);
        Paciente amarelo = criarPaciente("Amarelo", Risco.AMARELO);

        repository.saveAll(List.of(verde, vermelho, amarelo));

        List<Paciente> resultado = repository.buscarFilaDeEsperaOrdenada();

        assertThat(resultado).hasSize(3).containsExactly(vermelho, amarelo, verde);
    }

    private Paciente criarPaciente(String nome, Risco risco) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setRisco(risco);
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        return p;
    }
}
