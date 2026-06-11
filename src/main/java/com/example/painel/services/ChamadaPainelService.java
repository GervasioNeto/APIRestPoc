package com.example.painel.services;

import com.example.painel.dto.ChamadaPainelResponse;
import com.example.painel.entinty.ChamadaPainel;
import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoChamadaPainel;
import com.example.painel.events.ChamadaPainelCriadaEvent;
import com.example.painel.repository.ChamadaPainelRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChamadaPainelService {

    private final ChamadaPainelRepository chamadaPainelRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChamadaPainelService(ChamadaPainelRepository chamadaPainelRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.chamadaPainelRepository = chamadaPainelRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ChamadaPainelResponse registrarChamadaTriagem(Paciente paciente) {
        ChamadaPainel chamada = novaChamadaBase(paciente, TipoChamadaPainel.TRIAGEM);
        chamada.setDestino("Triagem");

        return salvarEPublicar(chamada);
    }

    @Transactional
    public ChamadaPainelResponse registrarChamadaConsultorio(Paciente paciente, Consultorio consultorio) {
        ChamadaPainel chamada = novaChamadaBase(paciente, TipoChamadaPainel.CONSULTORIO);
        chamada.setConsultorioId(consultorio.getId());
        chamada.setConsultorioNumero(consultorio.getNumero());
        chamada.setDestino(formatarDestinoConsultorio(consultorio));

        return salvarEPublicar(chamada);
    }

    @Transactional(readOnly = true)
    public List<ChamadaPainelResponse> listarRecentes() {
        return chamadaPainelRepository.findTop10ByOrderByCriadaEmDescIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ChamadaPainel novaChamadaBase(Paciente paciente, TipoChamadaPainel tipo) {
        ChamadaPainel chamada = new ChamadaPainel();
        chamada.setPacienteId(paciente.getId());
        chamada.setNomePaciente(paciente.getNome());
        chamada.setTicketNumber(gerarTicketNumber(paciente));
        chamada.setTipo(tipo);
        chamada.setRisco(paciente.getRisco());
        chamada.setCriadaEm(LocalDateTime.now());
        return chamada;
    }

    private ChamadaPainelResponse salvarEPublicar(ChamadaPainel chamada) {
        ChamadaPainel salva = chamadaPainelRepository.save(chamada);
        ChamadaPainelResponse response = toResponse(salva);

        eventPublisher.publishEvent(new ChamadaPainelCriadaEvent(response));

        return response;
    }

    private ChamadaPainelResponse toResponse(ChamadaPainel chamada) {
        return new ChamadaPainelResponse(
                chamada.getId(),
                chamada.getPacienteId(),
                chamada.getNomePaciente(),
                chamada.getTicketNumber(),
                mapTipo(chamada.getTipo()),
                chamada.getDestino(),
                chamada.getConsultorioId(),
                chamada.getConsultorioNumero(),
                mapRisco(chamada.getRisco()),
                chamada.getCriadaEm()
        );
    }

    private String gerarTicketNumber(Paciente paciente) {
        if (paciente.getId() == null) {
            return "P-0000";
        }

        return "P-%04d".formatted(paciente.getId());
    }

    private String formatarDestinoConsultorio(Consultorio consultorio) {
        Long numero = consultorio.getNumero();

        if (numero == null) {
            return "Consultorio";
        }

        return "Consultorio " + numero;
    }

    private String mapTipo(TipoChamadaPainel tipo) {
        if (tipo == TipoChamadaPainel.TRIAGEM) {
            return "triage";
        }

        return "doctor";
    }

    private String mapRisco(Risco risco) {
        if (risco == null) {
            return null;
        }

        return switch (risco) {
            case VERMELHO -> "red";
            case LARANJA -> "orange";
            case AMARELO -> "yellow";
            case VERDE -> "green";
            case AZUL -> "blue";
            case NAO_CLASSIFICADO -> null;
        };
    }
}
