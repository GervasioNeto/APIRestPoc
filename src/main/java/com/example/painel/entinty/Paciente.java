package com.example.painel.entinty;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import jakarta.persistence.*;


@Entity
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String nome;
    String cpf;

    @Enumerated(EnumType.STRING)
    private Risco risco;

    @Enumerated(EnumType.STRING)
    private TipoAtendimento tipo;

    @ManyToOne
    @JoinColumn(name = "triagem_id")
    private Triagem triagem;

    @ManyToOne
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    public Paciente() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Triagem getTriagem() {
        return triagem;
    }

    public void setTriagem(Triagem triagem) {
        this.triagem = triagem;
    }

    public Consultorio getConsultorio() {
        return consultorio;
    }

    public void setConsultorio(Consultorio consultorio) {
        this.consultorio = consultorio;
    }
}
