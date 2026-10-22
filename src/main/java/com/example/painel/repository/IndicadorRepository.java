package com.example.painel.repository;

import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IndicadorRepository extends JpaRepository<Paciente, Long> {

    // GERAL inclui tipo nulo: SQL "tipo <> SAMU" sozinho excluiria esses pacientes.
    String FILTRO_ESCOPO = " AND ((:samu = true AND p.tipo = 'SAMU') " +
            "OR (:samu = false AND (p.tipo IS NULL OR p.tipo <> 'SAMU'))) ";

    // ---- Grupo 0: tempos médios gerais (indicadores 1-3) ----

    // AVG ignora diferenças nulas: cada média usa só quem tem as suas duas datas.
    // Sem amostras, a linha única vem com null.
    @Query(nativeQuery = true, value =
            "SELECT AVG(EXTRACT(EPOCH FROM (p.chamada_triagem_at - p.chegada_at)) / 60.0) AS acolhimento_minutos, " +
            "       AVG(EXTRACT(EPOCH FROM (p.chamada_consultorio_at - p.classified_at)) / 60.0) AS classificacao_ate_medico_minutos, " +
            "       AVG(EXTRACT(EPOCH FROM (p.chamada_consultorio_at - p.chegada_at)) / 60.0) AS entrada_ate_medico_minutos " +
            "FROM paciente p " +
            "WHERE p.chegada_at BETWEEN :inicio AND :fim " + FILTRO_ESCOPO)
    TemposMedios temposMedios(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    // ---- Grupo 1: contagens simples (indicadores 4-13) ----

    @Query("SELECT COUNT(p) FROM Paciente p WHERE p.chegadaAt BETWEEN :inicio AND :fim" + FILTRO_ESCOPO)
    long countCadastrados(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    @Query("SELECT COUNT(p) FROM Paciente p " +
            "WHERE p.classifiedAt IS NOT NULL AND p.chegadaAt BETWEEN :inicio AND :fim" + FILTRO_ESCOPO)
    long countClassificados(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    @Query("SELECT COUNT(p) FROM Paciente p " +
            "WHERE p.status = 'FINALIZADO' AND p.chegadaAt BETWEEN :inicio AND :fim" + FILTRO_ESCOPO)
    long countAtendidos(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    @Query("SELECT p.risco AS risco, COUNT(p) AS total FROM Paciente p " +
            "WHERE p.chegadaAt BETWEEN :inicio AND :fim" + FILTRO_ESCOPO + "GROUP BY p.risco")
    List<RiscoContagem> contarPorRisco(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    // ---- Grupo 2: tempo médio de espera por risco/tipo (indicadores 14-20) ----

    @Query(nativeQuery = true, value =
            "SELECT risco, tipo, " +
            "       AVG(EXTRACT(EPOCH FROM (chamada_consultorio_at - classified_at)) / 60.0) AS tempo_medio_minutos " +
            "FROM paciente " +
            "WHERE classified_at IS NOT NULL " +
            "  AND chamada_consultorio_at IS NOT NULL " +
            "  AND chegada_at BETWEEN :inicio AND :fim " +
            "  AND ((:samu = true AND tipo = 'SAMU') " +
            "    OR (:samu = false AND (tipo IS NULL OR tipo <> 'SAMU'))) " +
            "GROUP BY risco, tipo")
    List<TempoMedioPorRiscoTipo> tempoMedioEsperaPorRiscoETipo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    // ---- Grupo 3: % fora do tempo de protocolo por risco/tipo (indicadores 21-27) ----

    @Query(nativeQuery = true, value =
            "SELECT p.risco AS risco, p.tipo AS tipo, " +
            "       COUNT(*) AS total, " +
            "       COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (p.chamada_consultorio_at - p.classified_at)) / 60.0 > pt.tempo_maximo_minutos) AS fora_do_prazo " +
            "FROM paciente p " +
            "JOIN protocolo_tempo pt ON pt.risco = p.risco AND pt.tipo = p.tipo " +
            "WHERE p.classified_at IS NOT NULL " +
            "  AND p.chamada_consultorio_at IS NOT NULL " +
            "  AND p.chegada_at BETWEEN :inicio AND :fim " + FILTRO_ESCOPO +
            "GROUP BY p.risco, p.tipo")
    List<ForaDoProtocoloPorRiscoTipo> contarForaDoProtocoloPorRiscoETipo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("samu") boolean samu);

    // ---- Projections ----

    interface TemposMedios {
        Double getAcolhimentoMinutos();
        Double getClassificacaoAteMedicoMinutos();
        Double getEntradaAteMedicoMinutos();
    }

    interface RiscoContagem {
        Risco getRisco();
        Long getTotal();
    }

    interface TempoMedioPorRiscoTipo {
        String getRisco();
        String getTipo();
        Double getTempoMedioMinutos();
    }

    interface ForaDoProtocoloPorRiscoTipo {
        String getRisco();
        String getTipo();
        Long getTotal();
        Long getForaDoPrazo();
    }
}
