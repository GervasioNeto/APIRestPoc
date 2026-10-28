package com.example.painel.services;

import com.example.painel.dto.ProtocoloTempoResponse;
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
}
