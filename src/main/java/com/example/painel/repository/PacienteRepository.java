    package com.example.painel.repository;

    import com.example.painel.entinty.Consultorio;
    import com.example.painel.entinty.Paciente;
    import com.example.painel.enums.Risco;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;

    import java.util.List;

    @Repository
    public interface PacienteRepository extends JpaRepository<Paciente, Long> {

        Paciente findByCpf(String cpf);

        // Reserva atômica do paciente para um consultório: só grava se nenhum consultório o chamou ainda.
        // Com dois médicos clicando juntos, o banco aplica um UPDATE de cada vez e o segundo afeta 0 linhas.
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE Paciente p SET p.consultorio = :consultorio WHERE p.id = :id AND p.consultorio IS NULL")
        int reservarParaConsultorio(@Param("id") Long id, @Param("consultorio") Consultorio consultorio);

        // Fila do médico: primeiro a cor; na mesma cor, quem chegou antes (chegadaAt, entrada no NAC).
        // O id só desempata chegadas no mesmo instante: ele é a ordem de cadastro, não de chegada.
        @Query("SELECT p FROM Paciente p " +
                "WHERE p.status = 'AGUARDANDO_CONSULTA' " +
                "AND p.consultorio IS NULL " +
                "ORDER BY " +
                "CASE p.risco " +
                "  WHEN 'VERMELHO' THEN 1 " +
                "  WHEN 'LARANJA' THEN 2 " +
                "  WHEN 'AMARELO' THEN 3 " +
                "  WHEN 'VERDE' THEN 4 " +
                "  WHEN 'AZUL' THEN 5 " +
                "  ELSE 6 " +
                "END ASC, " +
                "p.chegadaAt ASC, " +
                "p.id ASC")

        List<Paciente> buscarFilaDeEsperaOrdenada();

        List<Paciente> findByRisco(Risco risco);
    }
