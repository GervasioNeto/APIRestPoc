package com.example.painel.services;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

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
                .filter(p -> p.getRisco() == null)
                .collect(Collectors.toList());
    }

    @Transactional
    public Paciente classificar(Long id, Paciente dados) {
        Paciente p = buscarPorId(id);
        p.setRisco(dados.getRisco());
        p.setTipo(dados.getTipo());
        p.setTriageNotes(dados.getTriageNotes());
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        return pacienteRepository.save(p);
    }

    public List<Paciente> listarFilaMedica() {
        return pacienteRepository.buscarFilaDeEsperaOrdenada();
    }

    @Transactional
    public Paciente chamarParaConsultorio(Long id, Long consultorioId) {
        Paciente p = buscarPorId(id);
        Consultorio c = consultorioRepository.findById(consultorioId)
                .orElseThrow(() -> new RuntimeException("Consultório não encontrado"));

        p.setConsultorio(c);
        p.setStatus(TipoStatus.CHAMADO);

        return pacienteRepository.save(p);
    }

    @Transactional
    public Paciente finalizarAtendimento(Long id) {
        Paciente p = buscarPorId(id);
        p.setStatus(TipoStatus.FINALIZADO);
        return pacienteRepository.save(p);
    }

    @Transactional
    public Paciente registrarDesistencia(Long id) {
        Paciente p = buscarPorId(id);
        p.setStatus(TipoStatus.DESISTENCIA);
        return pacienteRepository.save(p);
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

        return pacienteRepository.save(p);
    }


    @Transactional
    public Paciente recolocarNaFila(Long id) {
        Paciente p = buscarPorId(id);

        p.setConsultorio(null);
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);

        return pacienteRepository.save(p);
    }
}