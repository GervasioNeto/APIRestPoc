package com.example.painel.controllers;

import com.example.painel.entinty.Paciente;
import com.example.painel.services.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    @PostMapping
    public ResponseEntity<Paciente> criar(@RequestBody Paciente paciente) {
        return ResponseEntity.ok(pacienteService.criar(paciente));
    }

    @GetMapping
    public ResponseEntity<List<Paciente>> listarTodos() {
        return ResponseEntity.ok(pacienteService.listarTodos());
    }

    @GetMapping("/aguardando-triagem")
    public ResponseEntity<List<Paciente>> listarParaTriagem() {
        return ResponseEntity.ok(pacienteService.listarAguardandoTriagem());
    }

    @PutMapping("/{id}/classificar")
    public ResponseEntity<Paciente> classificar(@PathVariable Long id, @RequestBody Paciente dados) {
        return ResponseEntity.ok(pacienteService.classificar(id, dados));
    }

    @GetMapping("/aguardando-medico")
    public ResponseEntity<List<Paciente>> listarFilaMedica() {
        return ResponseEntity.ok(pacienteService.listarFilaMedica());
    }

    @PutMapping("/{id}/chamar")
    public ResponseEntity<Paciente> chamar(@PathVariable Long id, @RequestParam Long consultorioId) {
        return ResponseEntity.ok(pacienteService.chamarParaConsultorio(id, consultorioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pacienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Paciente> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.finalizarAtendimento(id));
    }

    @PutMapping("/{id}/desistencia")
    public ResponseEntity<Paciente> desistencia(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.registrarDesistencia(id));
    }
}