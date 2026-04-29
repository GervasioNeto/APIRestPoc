package com.example.painel.services;

import com.example.painel.dto.ChamadaPainelResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PainelChamadasSseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        emitters.add(emitter);
        sendEvent(emitter, "connected", "ok");

        return emitter;
    }

    public void publish(ChamadaPainelResponse chamada) {
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, "patient-called", chamada, String.valueOf(chamada.id()));
        }
    }

    @Scheduled(fixedRate = 25000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, "ping", Instant.now().toString());
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        sendEvent(emitter, eventName, data, null);
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data, String id) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name(eventName)
                    .data(data);

            if (id != null) {
                event.id(id);
            }

            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitter);
        }
    }
}
