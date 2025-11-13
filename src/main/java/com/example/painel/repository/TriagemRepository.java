package com.example.painel.repository;

import com.example.painel.entinty.Triagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriagemRepository extends JpaRepository<Triagem, Long> {

    // Geralmente operações padrão de CRUD (save, delete, findAll) são suficientes aqui
    // Mas se precisar buscar pelo número do guichê de triagem:
    Triagem findByNumero(Long numero);
}
