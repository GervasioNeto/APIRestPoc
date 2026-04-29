package com.example.painel.services;

import com.example.painel.events.ChamadaPainelCriadaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChamadaPainelEventListener {

    private final PainelChamadasSseService sseService;

    public ChamadaPainelEventListener(PainelChamadasSseService sseService) {
        this.sseService = sseService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChamadaCriada(ChamadaPainelCriadaEvent event) {
        sseService.publish(event.chamada());
    }
}
