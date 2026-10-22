package com.example.painel.services;

import com.example.painel.dto.HistoricoChamadaResponse;
import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.ProtocoloTempoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PacienteService {

    private static final String NOME_ANONIMO = "Paciente Anônimo";

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private ChamadaPainelService chamadaPainelService;

    @Autowired
    private ProtocoloTempoRepository protocoloTempoRepository;

    // CRUD Básico
    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado com o ID: " + id));
    }

    @Transactional
    public Paciente criar(Paciente paciente) {
        paciente.setStatus(TipoStatus.AGUARDANDO_TRIAGEM);
        paciente.setChegadaAt(LocalDateTime.now());
        return pacienteRepository.save(paciente);
    }

    @Transactional
    public void deletar(Long id) {
        Paciente p = buscarPorId(id);
        anonimizar(p);
        pacienteRepository.save(p);
    }

    // Regras de Negócio Específicas
    public List<Paciente> listarAguardandoTriagem() {
        return pacienteRepository.findAll().stream()
                .filter(p -> p.getRisco() == null
                        && p.getStatus() != TipoStatus.FINALIZADO
                        && p.getStatus() != TipoStatus.DESISTENCIA)
                .collect(Collectors.toList());
    }

    @Transactional
    public Paciente classificar(Long id, Paciente dados) {
        Paciente p = buscarPorId(id);
        p.setRisco(dados.getRisco());
        p.setTipo(dados.getTipo());
        p.setTriageNotes(dados.getTriageNotes());
        p.setClassifiedAt(LocalDateTime.now());
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        return pacienteRepository.save(p);
    }

    public List<Paciente> listarFilaMedica() {
        List<Paciente> fila = pacienteRepository.buscarFilaDeEsperaOrdenada();
        if (fila.isEmpty()) {
            return fila;
        }

        // Carrega o protocolo uma vez por requisição (evita N+1)
        Map<ChaveProtocolo, Integer> tempoMaximoPorRiscoETipo = protocoloTempoRepository.findAll().stream()
                .filter(pt -> pt.getTempoMaximoMinutos() != null)
                .collect(Collectors.toMap(
                        pt -> new ChaveProtocolo(pt.getRisco(), pt.getTipo()),
                        ProtocoloTempo::getTempoMaximoMinutos,
                        (primeiro, segundo) -> primeiro));

        fila.forEach(p -> p.setPrazoAtendimentoAt(calcularPrazoAtendimento(p, tempoMaximoPorRiscoETipo)));
        return fila;
    }

    // Só informa o prazo; a comparação com o horário atual (atraso) fica no front
    private LocalDateTime calcularPrazoAtendimento(Paciente p, Map<ChaveProtocolo, Integer> tempoMaximoPorRiscoETipo) {
        if (p.getClassifiedAt() == null) {
            return null;
        }

        Integer tempoMaximo = tempoMaximoPorRiscoETipo.get(new ChaveProtocolo(p.getRisco(), p.getTipo()));
        if (tempoMaximo == null) {
            return null;
        }

        return p.getClassifiedAt().plusMinutes(tempoMaximo);
    }

    private record ChaveProtocolo(Risco risco, TipoAtendimento tipo) {}

    @Transactional
    public Paciente chamarParaTriagem(Long id) {
        Paciente p = buscarPorId(id);

        p.setStatus(TipoStatus.CHAMADO);

        if (p.getChamadaTriagemAt() == null) {
            p.setChamadaTriagemAt(LocalDateTime.now());
        }

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaTriagem(salvo);

        return salvo;
    }

    @Transactional
    public Paciente chamarParaConsultorio(Long id, Long consultorioId) {
        Paciente p = buscarPorId(id);
        Consultorio c = consultorioRepository.findById(consultorioId)
                .orElseThrow(() -> new RuntimeException("Consultório não encontrado"));

        p.setConsultorio(c);
        p.setStatus(TipoStatus.CHAMADO);

        if (p.getChamadaConsultorioAt() == null) {
            p.setChamadaConsultorioAt(LocalDateTime.now());
        }

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaConsultorio(salvo, c);

        return salvo;
    }

    @Transactional
    public void finalizarAtendimento(Long id) {
        Paciente p = buscarPorId(id);
        p.setStatus(TipoStatus.FINALIZADO);
        p.setAtendimentoFinalizadoAt(LocalDateTime.now());
        anonimizar(p);
        pacienteRepository.save(p);
        chamadaPainelService.anonimizarChamadasDoPaciente(p.getId(), NOME_ANONIMO);
    }

    @Transactional
    public void registrarDesistencia(Long id) {
        Paciente p = buscarPorId(id);
        p.setEtapaDesistencia(p.getStatus());
        p.setStatus(TipoStatus.DESISTENCIA);
        p.setAtendimentoFinalizadoAt(LocalDateTime.now());
        anonimizar(p);
        pacienteRepository.save(p);
        chamadaPainelService.anonimizarChamadasDoPaciente(p.getId(), NOME_ANONIMO);
    }


    @Transactional
    public Paciente rechamarTriagem(Long id) {
        Paciente p = buscarPorId(id);

        if (p.getChamadaTriagemAt() == null || p.getStatus() != TipoStatus.CHAMADO
                || p.getRisco() != null || p.getConsultorio() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Paciente não está aguardando triagem após chamada");
        }

        p.setRechamadasTriagemCount(
                (p.getRechamadasTriagemCount() == null ? 0 : p.getRechamadasTriagemCount()) + 1);

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaTriagem(salvo);
        return salvo;
    }

    @Transactional
    public Paciente rechamarPaciente(Long id) {
        Paciente p = buscarPorId(id);

        if (p.getConsultorio() == null) {
            throw new RuntimeException("Paciente não está em atendimento");
        }

        // NÃO remove da consulta
        // Apenas dispara novo chamado
        p.setStatus(TipoStatus.CHAMADO);
        p.setRechamadasConsultorioCount(
                (p.getRechamadasConsultorioCount() == null ? 0 : p.getRechamadasConsultorioCount()) + 1);

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaConsultorio(salvo, salvo.getConsultorio());

        return salvo;
    }


    public List<HistoricoChamadaResponse> listarHistoricoChamadas(Long id) {
        if (!pacienteRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Paciente não encontrado com o ID: " + id);
        }

        return chamadaPainelService.listarHistoricoDoPaciente(id);
    }

    @Transactional
    public Paciente recolocarNaFila(Long id) {
        Paciente p = buscarPorId(id);

        p.setConsultorio(null);
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);

        return pacienteRepository.save(p);
    }

    private void anonimizar(Paciente p) {
        p.setNome(NOME_ANONIMO);
        p.setCpf("000.000.000-00");
        p.setTriageNotes(null);
    }
}
