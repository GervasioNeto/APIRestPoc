package com.example.painel.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Fuso do hospital fixo em America/Fortaleza (BE-06). Os horários do paciente e das chamadas são
 * LocalDateTime gravados sem fuso: se viessem do relógio do servidor, um servidor ou container em UTC
 * gravaria tudo 3 horas adiantado (indicadores, filtro por horário, prazo da fila e cronômetros).
 * Usar sempre LocalDateTime.now(clock) com este Clock, nunca LocalDateTime.now().
 */
@Configuration
public class HorarioConfig {

    public static final ZoneId FUSO_HOSPITAL = ZoneId.of("America/Fortaleza");

    /** Relógio padrão do sistema, no fuso do hospital. Nos testes dá para trocar por Clock.fixed. */
    public static Clock relogioDoHospital() {
        return Clock.system(FUSO_HOSPITAL);
    }

    @Bean
    public Clock clock() {
        return relogioDoHospital();
    }
}
