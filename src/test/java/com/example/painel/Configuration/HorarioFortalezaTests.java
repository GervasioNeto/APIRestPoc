package com.example.painel.Configuration;

import com.example.painel.entinty.ChamadaPainel;
import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.enums.ComportamentoSuicida;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ChamadaPainelRepository;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.ProtocoloTempoRepository;
import com.example.painel.services.ChamadaPainelService;
import com.example.painel.services.PacienteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// BE-06: os horários gravados seguem America/Fortaleza mesmo com a JVM (servidor/container) em UTC.
class HorarioFortalezaTests {

    // 25/11/2026 15:00 em UTC = 12:00 em Fortaleza (UTC-3, sem horário de verão).
    private static final Instant INSTANTE = Instant.parse("2026-11-25T15:00:00Z");
    private static final LocalDateTime MEIO_DIA_FORTALEZA = LocalDateTime.of(2026, 11, 25, 12, 0);

    private TimeZone fusoOriginal;
    private PacienteRepository pacienteRepository;
    private ChamadaPainelRepository chamadaPainelRepository;
    private PacienteService pacienteService;
    private Paciente paciente;

    @BeforeEach
    void jvmEmUtc() {
        fusoOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Clock relogio = Clock.fixed(INSTANTE, HorarioConfig.FUSO_HOSPITAL);
        pacienteRepository = mock(PacienteRepository.class);
        chamadaPainelRepository = mock(ChamadaPainelRepository.class);
        ConsultorioRepository consultorioRepository = mock(ConsultorioRepository.class);
        ChamadaPainelService chamadaPainelService =
                new ChamadaPainelService(chamadaPainelRepository, mock(ApplicationEventPublisher.class), relogio);

        pacienteService = new PacienteService();
        ReflectionTestUtils.setField(pacienteService, "pacienteRepository", pacienteRepository);
        ReflectionTestUtils.setField(pacienteService, "consultorioRepository", consultorioRepository);
        ReflectionTestUtils.setField(pacienteService, "chamadaPainelService", chamadaPainelService);
        ReflectionTestUtils.setField(pacienteService, "protocoloTempoRepository", mock(ProtocoloTempoRepository.class));
        ReflectionTestUtils.setField(pacienteService, "clock", relogio);

        paciente = new Paciente();
        paciente.setId(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chamadaPainelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consultorioRepository.findById(2L)).thenReturn(Optional.of(new Consultorio()));
        when(pacienteRepository.reservarParaConsultorio(anyLong(), any())).thenReturn(1);
    }

    @AfterEach
    void restauraFuso() {
        TimeZone.setDefault(fusoOriginal);
    }

    @Test
    void oRelogioDaAplicacaoEstaEmFortaleza() {
        assertThat(new HorarioConfig().clock().getZone()).isEqualTo(ZoneId.of("America/Fortaleza"));
    }

    @Test
    void todosOsHorariosDoPacienteSaoGravadosNoHorarioDeFortaleza() {
        Paciente novo = pacienteService.criar(new Paciente());
        assertThat(novo.getChegadaAt()).isEqualTo(MEIO_DIA_FORTALEZA);

        paciente.setStatus(TipoStatus.AGUARDANDO_TRIAGEM);
        pacienteService.chamarParaTriagem(1L);
        assertThat(paciente.getChamadaTriagemAt()).isEqualTo(MEIO_DIA_FORTALEZA);

        Paciente classificacao = new Paciente();
        classificacao.setRisco(Risco.LARANJA);
        classificacao.setTipo(TipoAtendimento.CLINICO);
        pacienteService.classificar(1L, classificacao);
        assertThat(paciente.getClassifiedAt()).isEqualTo(MEIO_DIA_FORTALEZA);

        pacienteService.chamarParaConsultorio(1L, 2L);
        assertThat(paciente.getChamadaConsultorioAt()).isEqualTo(MEIO_DIA_FORTALEZA);

        pacienteService.finalizarAtendimento(1L, ComportamentoSuicida.NENHUM, null);
        assertThat(paciente.getAtendimentoFinalizadoAt()).isEqualTo(MEIO_DIA_FORTALEZA);
    }

    @Test
    void desistenciaEGravadaNoHorarioDeFortaleza() {
        paciente.setStatus(TipoStatus.AGUARDANDO_CONSULTA);

        pacienteService.registrarDesistencia(1L);

        assertThat(paciente.getAtendimentoFinalizadoAt()).isEqualTo(MEIO_DIA_FORTALEZA);
    }

    @Test
    void chamadaNoPainelEGravadaNoHorarioDeFortaleza() {
        paciente.setStatus(TipoStatus.AGUARDANDO_TRIAGEM);

        pacienteService.chamarParaTriagem(1L);

        verify(chamadaPainelRepository).save(argThatCriadaEm(MEIO_DIA_FORTALEZA));
    }

    @Test
    void comORelogioRealEAJvmEmUtcAChegadaFicaTresHorasAtrasDoUtc() {
        ReflectionTestUtils.setField(pacienteService, "clock", HorarioConfig.relogioDoHospital());

        LocalDateTime chegada = pacienteService.criar(new Paciente()).getChegadaAt();

        assertThat(chegada).isCloseTo(LocalDateTime.now(ZoneId.of("America/Fortaleza")), within(5, ChronoUnit.SECONDS));
        // Sem o fuso fixo, LocalDateTime.now() com a JVM em UTC daria 3 horas a mais.
        assertThat(ChronoUnit.MINUTES.between(chegada, LocalDateTime.now())).isBetween(179L, 181L);
    }

    @Test
    void chamadaSemHorarioGanhaOHorarioDeFortalezaAoSerGravada() throws Exception {
        ChamadaPainel chamada = new ChamadaPainel();
        Method prePersist = ChamadaPainel.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);

        prePersist.invoke(chamada);

        assertThat(chamada.getCriadaEm())
                .isCloseTo(LocalDateTime.now(ZoneId.of("America/Fortaleza")), within(5, ChronoUnit.SECONDS));
    }

    private static ChamadaPainel argThatCriadaEm(LocalDateTime esperado) {
        return org.mockito.ArgumentMatchers.argThat(c -> c != null && esperado.equals(c.getCriadaEm()));
    }
}
