package com.example.painel.enums;

public enum Risco {
    VERMELHO(1),
    LARANJA(2),
    AMARELO(3),
    VERDE(4),
    AZUL(5),
    NAO_CLASSIFICADO(99);

    private int prioridade;

    Risco(int prioridade) {
        this.prioridade = prioridade;
    }

    public int getPrioridade() { return prioridade; }
}
