package com.example.painel.services;

import com.example.painel.dto.ChamadaPainelResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PainelChamadasSseServiceConcurrencyTest {

    @Test
    @DisplayName("Garante que múltiplas chamadas concorrentes publicam eventos sem falhas no SseEmitter")
    void deveEnviarTodosEventosSemPerdaSobConcorrencia() throws Exception {
        PainelChamadasSseService sseService = new PainelChamadasSseService();
        SseEmitter emitter = sseService.subscribe();

        int totalThreads = 30;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalThreads);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= totalThreads; i++) {
            final long id = i;
            futures.add(executor.submit(() -> {
                try {
                    startGate.await();
                    ChamadaPainelResponse chamada = new ChamadaPainelResponse(
                            id,
                            id,
                            "Paciente " + id,
                            "P-000" + id,
                            "doctor",
                            "Consultorio " + id,
                            id,
                            id,
                            "yellow",
                            LocalDateTime.now()
                    );
                    sseService.publish(chamada);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    endGate.countDown();
                }
            }));
        }

        // Dispara todas as threads simultaneamente
        startGate.countDown();
        boolean completedInTime = endGate.await(5, TimeUnit.SECONDS);

        executor.shutdown();

        assertThat(completedInTime).isTrue();
        for (Future<?> future : futures) {
            assertThat(future.get()).isNull();
        }
    }
}
