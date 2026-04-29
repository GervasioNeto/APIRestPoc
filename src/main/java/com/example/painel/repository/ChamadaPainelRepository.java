package com.example.painel.repository;

import com.example.painel.entinty.ChamadaPainel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamadaPainelRepository extends JpaRepository<ChamadaPainel, Long> {

    List<ChamadaPainel> findTop10ByOrderByCriadaEmDescIdDesc();
}
