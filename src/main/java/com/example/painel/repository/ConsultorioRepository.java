package com.example.painel.repository;

import com.example.painel.entinty.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {

    // Para encontrar o consultório pelo número (Ex: Sala 104) em vez do ID do banco
    Optional<Consultorio> findByNumero(Long numero);
}
