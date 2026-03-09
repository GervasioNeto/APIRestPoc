package com.example.painel.repository;

import com.example.painel.entinty.Triagem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TriagemRepositoryTest {

    @Autowired
    private TriagemRepository triagemRepository;

    @Test
    @DisplayName("Deve retornar triagem quando buscar por número existente")
    void deveRetornarTriagemPorNumero() {
        Triagem triagem = new Triagem();
        triagem.setNumero(101L);
        triagemRepository.save(triagem);

        Triagem resultado = triagemRepository.findByNumero(101L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNumero()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Deve retornar null quando buscar por número inexistente")
    void deveRetornarNullPorNumeroInexistente() {
        Triagem resultado = triagemRepository.findByNumero(999L);

        assertThat(resultado).isNull();
    }
}
