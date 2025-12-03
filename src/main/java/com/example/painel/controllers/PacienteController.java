package com.example.painel.controllers;

import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.TriagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private TriagemRepository triagemRepository;

    @PostMapping
    public ResponseEntity<Paciente> criar(@RequestBody Paciente paciente) {
        return ResponseEntity.ok(pacienteRepository.save(paciente));
    }

    @GetMapping("/aguardando-triagem")
    public ResponseEntity<List<Paciente>> listarParaTriagem() {
        List<Paciente> lista = pacienteRepository.findAll().stream()
                .filter(p -> p.getRisco() == null)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}/classificar")
    public ResponseEntity<Paciente> classificar(
            @PathVariable Long id,
            @RequestBody Paciente dadosParciais) {

        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        p.setRisco(dadosParciais.getRisco());
        p.setTipo(dadosParciais.getTipo());
        p.setTriageNotes(dadosParciais.getTriageNotes());

        return ResponseEntity.ok(pacienteRepository.save(p));
    }

    @GetMapping("/aguardando-medico")
    public ResponseEntity<List<Paciente>> listarFilaMedica() {
        return ResponseEntity.ok(pacienteRepository.buscarFilaDeEsperaOrdenada());
    }

    @PutMapping("/{id}/chamar")
    public ResponseEntity<Paciente> chamar(
            @PathVariable Long id,
            @RequestParam Long consultorioId) {

        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        Consultorio c = consultorioRepository.findById(consultorioId)
                .orElseThrow(() -> new RuntimeException("Consultório não encontrado"));

        p.setConsultorio(c);

        return ResponseEntity.ok(pacienteRepository.save(p));
    }
}
