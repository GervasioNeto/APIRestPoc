package com.example.painel.services;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private ChamadaPainelService chamadaPainelService;

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
        pacienteRepository.delete(p);
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
        return pacienteRepository.buscarFilaDeEsperaOrdenada();
    }

    @Transactional
    public Paciente chamarParaTriagem(Long id) {
        Paciente p = buscarPorId(id);

        p.setStatus(TipoStatus.CHAMADO);

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

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaConsultorio(salvo, c);

        return salvo;
    }

    @Transactional
    public void finalizarAtendimento(Long id) {
        Paciente p = buscarPorId(id);
        pacienteRepository.delete(p);
    }

    @Transactional
    public void registrarDesistencia(Long id) {
        Paciente p = buscarPorId(id);
        pacienteRepository.delete(p);
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

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaConsultorio(salvo, salvo.getConsultorio());

        return salvo;
    }


    @Transactional
    public Paciente recolocarNaFila(Long id) {
        Paciente p = buscarPorId(id);

        p.setConsultorio(null);
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);

        return pacienteRepository.save(p);
    }

    private void anonimizar(Paciente p) {
        p.setNome("Paciente Anônimo");
        p.setCpf("000.000.000-00");
        p.setTriageNotes(null);
    }
}
