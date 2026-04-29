package com.example.painel.services;

import com.example.painel.dto.ChamadaPainelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PainelChamadasSseService {

    private static final Logger log = LoggerFactory.getLogger(PainelChamadasSseService.class);

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
            removeEmitterAfterSendFailure(emitter, eventName, id, ex);
        }
    }

    private void removeEmitterAfterSendFailure(SseEmitter emitter, String eventName, String id, Exception ex) {
        log.warn("Removing SSE emitter after failing to send event. eventName={}, eventId={}", eventName, id, ex);

        try {
            emitter.completeWithError(ex);
        } catch (IllegalStateException completeEx) {
            log.debug("SSE emitter was already completed while handling send failure. eventName={}, eventId={}",
                    eventName, id, completeEx);
        } finally {
            emitters.remove(emitter);
        }
    }
}
