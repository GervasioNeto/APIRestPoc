package com.example.painel.entinty;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Triagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long numero;

    @OneToMany(mappedBy = "triagem")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Paciente> pacientes;

    public Triagem() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public List<Paciente> getPacientes() {
        return pacientes;
    }

    public void setPacientes(List<Paciente> pacientes) {
        this.pacientes = pacientes;
    }
}
