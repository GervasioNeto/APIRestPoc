package com.example.painel.entinty;

import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ProtocoloTempo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Risco risco;

    @Enumerated(EnumType.STRING)
    private TipoAtendimento tipo;

    private Integer tempoMaximoMinutos;

    public ProtocoloTempo() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Risco getRisco() {
        return risco;
    }

    public void setRisco(Risco risco) {
        this.risco = risco;
    }

    public TipoAtendimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoAtendimento tipo) {
        this.tipo = tipo;
    }

    public Integer getTempoMaximoMinutos() {
        return tempoMaximoMinutos;
    }

    public void setTempoMaximoMinutos(Integer tempoMaximoMinutos) {
        this.tempoMaximoMinutos = tempoMaximoMinutos;
    }
}
