package com.example.painel.repository;

import com.example.painel.entinty.Consultorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ConsultorioRepositoryTest {

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Test
    @DisplayName("Deve retornar consultório quando buscar por número existente")
    void deveRetornarConsultorioPorNumero() {
        Consultorio consultorio = new Consultorio();
        consultorio.setNumero(101L);
        consultorioRepository.save(consultorio);

        Optional<Consultorio> resultado = consultorioRepository.findByNumero(101L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNumero()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Deve retornar vazio quando buscar por número inexistente")
    void deveRetornarVazioPorNumeroInexistente() {
        Optional<Consultorio> resultado = consultorioRepository.findByNumero(999L);

        assertThat(resultado).isEmpty();
    }
}
