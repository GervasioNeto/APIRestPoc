package com.example.painel.repository;

import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Paciente findByCpf(String cpf);

    @Query("SELECT p FROM Paciente p " +
            "WHERE p.consultorio IS NULL " +
            "AND p.risco IS NOT NULL " +
            "ORDER BY " +
            "CASE p.risco " +
            "  WHEN 'VERMELHO' THEN 1 " +
            "  WHEN 'LARANJA' THEN 2 " +
            "  WHEN 'AMARELO' THEN 3 " +
            "  WHEN 'VERDE' THEN 4 " +
            "  WHEN 'AZUL' THEN 5 " +
            "  ELSE 6 " +
            "END ASC, " +
            "p.id ASC")

    List<Paciente> buscarFilaDeEsperaOrdenada();

    List<Paciente> findByRisco(Risco risco);
}
