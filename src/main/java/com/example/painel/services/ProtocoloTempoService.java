package com.example.painel.services;

import com.example.painel.dto.ProtocoloTempoResponse;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.repository.ProtocoloTempoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ProtocoloTempoService {
    private final ProtocoloTempoRepository repository;

    public ProtocoloTempoService(ProtocoloTempoRepository repository) {
        this.repository = repository;
    }

    /**
     * Combinações risco × tipo do protocolo com o tempo máximo de espera em minutos.
     * Linhas incompletas são ignoradas, como no cálculo do prazo da fila médica.
     * Ordem: tipo de atendimento e, dentro dele, prioridade do risco (vermelho primeiro).
     */
    @Transactional(readOnly = true)
    public List<ProtocoloTempoResponse> listar() {
        return repository.findAll().stream()
                .filter(pt -> pt.getRisco() != null && pt.getTipo() != null && pt.getTempoMaximoMinutos() != null)
                .map(pt -> new ProtocoloTempoResponse(pt.getRisco(), pt.getTipo(), pt.getTempoMaximoMinutos()))
                .sorted(Comparator.comparing(ProtocoloTempoResponse::tipo)
                        .thenComparingInt(p -> p.risco().getPrioridade()))
                .toList();
    }

    /**
     * Cadastra o protocolo padrão quando a tabela está vazia (banco novo), pra Triagem
     * nunca ficar sem cores. Não mexe em protocolos já cadastrados, mesmo que alterados à mão.
     * Retorna quantas linhas foram gravadas.
     */
    @Transactional
    public int cadastrarProtocoloPadraoSeVazio() {
        if (repository.count() > 0) {
            return 0;
        }
        List<ProtocoloTempo> padrao = protocoloPadrao();
        repository.saveAll(padrao);
        return padrao.size();
    }

    // Valores confirmados com o hospital em 2026-08-21; os mesmos de docs/indicadores/schema.sql.
    private static List<ProtocoloTempo> protocoloPadrao() {
        return List.of(
                protocolo(Risco.VERMELHO, TipoAtendimento.CLINICO, 0),
                protocolo(Risco.LARANJA, TipoAtendimento.CLINICO, 10),
                protocolo(Risco.VERMELHO, TipoAtendimento.PSIQUIATRICO, 0),
                protocolo(Risco.LARANJA, TipoAtendimento.PSIQUIATRICO, 10),
                protocolo(Risco.AMARELO, TipoAtendimento.PSIQUIATRICO, 60),
                protocolo(Risco.VERDE, TipoAtendimento.PSIQUIATRICO, 120),
                protocolo(Risco.AZUL, TipoAtendimento.PSIQUIATRICO, 240),
                protocolo(Risco.LARANJA, TipoAtendimento.SAMU, 10));
    }

    private static ProtocoloTempo protocolo(Risco risco, TipoAtendimento tipo, int tempoMaximoMinutos) {
        ProtocoloTempo protocolo = new ProtocoloTempo();
        protocolo.setRisco(risco);
        protocolo.setTipo(tipo);
        protocolo.setTempoMaximoMinutos(tempoMaximoMinutos);
        return protocolo;
    }
}
