package com.example.painel.entinty;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoChamadaPainel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;

@Entity
public class ChamadaPainel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pacienteId;
    private String nomePaciente;
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    private TipoChamadaPainel tipo;

    private String destino;
    private Long consultorioId;
    private Long consultorioNumero;

    @Enumerated(EnumType.STRING)
    private Risco risco;

    private LocalDateTime criadaEm;

    @PrePersist
    void prePersist() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getNomePaciente() {
        return nomePaciente;
    }

    public void setNomePaciente(String nomePaciente) {
        this.nomePaciente = nomePaciente;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public TipoChamadaPainel getTipo() {
        return tipo;
    }

    public void setTipo(TipoChamadaPainel tipo) {
        this.tipo = tipo;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public Long getConsultorioId() {
        return consultorioId;
    }

    public void setConsultorioId(Long consultorioId) {
        this.consultorioId = consultorioId;
    }

    public Long getConsultorioNumero() {
        return consultorioNumero;
    }

    public void setConsultorioNumero(Long consultorioNumero) {
        this.consultorioNumero = consultorioNumero;
    }

    public Risco getRisco() {
        return risco;
    }

    public void setRisco(Risco risco) {
        this.risco = risco;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }
}
