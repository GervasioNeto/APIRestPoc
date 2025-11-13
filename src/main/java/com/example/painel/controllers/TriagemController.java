package com.example.painel.controllers;

import com.example.painel.entinty.Triagem;
import com.example.painel.repository.TriagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/triagens")
public class TriagemController {

    @Autowired
    private TriagemRepository repository;

    @GetMapping
    public List<Triagem> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Triagem criar(@RequestBody Triagem triagem) {
        return repository.save(triagem);
    }

    @PutMapping("/{id}")
    public Triagem atualizar(@PathVariable Long id, @RequestBody Triagem triagem) {
        triagem.setId(id);
        return repository.save(triagem);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
