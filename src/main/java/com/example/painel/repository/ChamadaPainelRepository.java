package com.example.painel.repository;

import com.example.painel.entinty.ChamadaPainel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ChamadaPainelRepository extends JpaRepository<ChamadaPainel, Long> {

    List<ChamadaPainel> findTop10ByOrderByCriadaEmDescIdDesc();

    List<ChamadaPainel> findByPacienteIdOrderByCriadaEmAsc(Long pacienteId);

    // Uma consulta para todos os atendidos da lista (evita N+1).
    List<ChamadaPainel> findByPacienteIdInOrderByCriadaEmAscIdAsc(Collection<Long> pacienteIds);

    @Modifying
    @Query("UPDATE ChamadaPainel c SET c.nomePaciente = :nome WHERE c.pacienteId = :pacienteId")
    int anonimizarNomePorPacienteId(@Param("pacienteId") Long pacienteId, @Param("nome") String nome);
}
