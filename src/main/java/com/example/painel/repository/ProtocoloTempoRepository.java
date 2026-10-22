package com.example.painel.repository;

import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProtocoloTempoRepository extends JpaRepository<ProtocoloTempo, Long> {

    Optional<ProtocoloTempo> findByRiscoAndTipo(Risco risco, TipoAtendimento tipo);
}
