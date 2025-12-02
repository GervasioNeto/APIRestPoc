package com.example.painel.controllers;

import com.example.painel.entinty.Consultorio;
import com.example.painel.repository.ConsultorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consultorios")
public class ConsultorioController {

    @Autowired
    private ConsultorioRepository repository;

    @GetMapping
    public List<Consultorio> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Consultorio buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consultório não encontrado"));
    }

    @PostMapping
    public Consultorio criar(@RequestBody Consultorio consultorio) {
        return repository.save(consultorio);
    }

    @PutMapping("/{id}")
    public Consultorio atualizar(@PathVariable Long id, @RequestBody Consultorio consultorio) {
        consultorio.setId(id);
        return repository.save(consultorio);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }
}

