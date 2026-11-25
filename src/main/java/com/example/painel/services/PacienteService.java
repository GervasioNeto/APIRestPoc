package com.example.painel.services;

import com.example.painel.Configuration.HorarioConfig;
import com.example.painel.dto.HistoricoChamadaResponse;
import com.example.painel.entinty.Consultorio;
import com.example.painel.entinty.Paciente;
import com.example.painel.entinty.ProtocoloTempo;
import com.example.painel.enums.ComportamentoSuicida;
import com.example.painel.enums.Risco;
import com.example.painel.enums.TipoAtendimento;
import com.example.painel.enums.TipoStatus;
import com.example.painel.repository.ConsultorioRepository;
import com.example.painel.repository.PacienteRepository;
import com.example.painel.repository.ProtocoloTempoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PacienteService {

    private static final String NOME_ANONIMO = "Paciente Anônimo";

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private ChamadaPainelService chamadaPainelService;

    @Autowired
    private ProtocoloTempoRepository protocoloTempoRepository;

    // Fuso fixo do hospital (BE-06); o valor inicial vale para quem instancia sem o Spring.
    @Autowired
    private Clock clock = HorarioConfig.relogioDoHospital();

    // CRUD Básico
    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado com o ID: " + id));
    }

    @Transactional
    public Paciente criar(Paciente paciente) {
        paciente.setStatus(TipoStatus.AGUARDANDO_TRIAGEM);
        paciente.setChegadaAt(LocalDateTime.now(clock));
        return pacienteRepository.save(paciente);
    }

    @Transactional
    public void deletar(Long id) {
        Paciente p = buscarPorId(id);
        anonimizar(p);
        pacienteRepository.save(p);
    }

    // Regras de Negócio Específicas
    public List<Paciente> listarAguardandoTriagem() {
        return pacienteRepository.buscarFilaDeAcolhimentoOrdenada();
    }

    @Transactional
    public Paciente classificar(Long id, Paciente dados) {
        Paciente p = buscarPorId(id);
        // Aguardando consulta sem consultório é a reclassificação feita no acolhimento.
        exigirEtapa(p, "classificar", Etapa.CHAMADO_ACOLHIMENTO, Etapa.AGUARDANDO_CONSULTA);
        p.setRisco(dados.getRisco());
        p.setTipo(dados.getTipo());
        p.setTriageNotes(dados.getTriageNotes());
        p.setClassifiedAt(LocalDateTime.now(clock));
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);
        return pacienteRepository.save(p);
    }

    public List<Paciente> listarFilaMedica() {
        List<Paciente> fila = pacienteRepository.buscarFilaDeEsperaOrdenada();
        if (fila.isEmpty()) {
            return fila;
        }

        // Carrega o protocolo uma vez por requisição (evita N+1)
        Map<ChaveProtocolo, Integer> tempoMaximoPorRiscoETipo = protocoloTempoRepository.findAll().stream()
                .filter(pt -> pt.getTempoMaximoMinutos() != null)
                .collect(Collectors.toMap(
                        pt -> new ChaveProtocolo(pt.getRisco(), pt.getTipo()),
                        ProtocoloTempo::getTempoMaximoMinutos,
                        (primeiro, segundo) -> primeiro));

        fila.forEach(p -> p.setPrazoAtendimentoAt(calcularPrazoAtendimento(p, tempoMaximoPorRiscoETipo)));
        return fila;
    }

    // Só informa o prazo; a comparação com o horário atual (atraso) fica no front
    private LocalDateTime calcularPrazoAtendimento(Paciente p, Map<ChaveProtocolo, Integer> tempoMaximoPorRiscoETipo) {
        if (p.getClassifiedAt() == null) {
            return null;
        }

        Integer tempoMaximo = tempoMaximoPorRiscoETipo.get(new ChaveProtocolo(p.getRisco(), p.getTipo()));
        if (tempoMaximo == null) {
            return null;
        }

        return p.getClassifiedAt().plusMinutes(tempoMaximo);
    }

    private record ChaveProtocolo(Risco risco, TipoAtendimento tipo) {}

    // ---- Etapas (BE-04) ----
    // Cada ação só vale numa etapa. Fora dela responde 409 sem gravar nada nem chamar no painel:
    // tela desatualizada ou clique duplo não podem, por exemplo, finalizar duas vezes ou chamar quem desistiu.
    enum Etapa {
        AGUARDANDO_ACOLHIMENTO("aguardando acolhimento"),
        CHAMADO_ACOLHIMENTO("chamado para o acolhimento"),
        AGUARDANDO_CONSULTA("aguardando consulta"),
        CHAMADO_CONSULTORIO("chamado ao consultório"),
        FINALIZADO("com o atendimento finalizado"),
        DESISTIU("com desistência registrada");

        private final String descricao;

        Etapa(String descricao) { this.descricao = descricao; }
    }

    static Etapa etapaDe(Paciente p) {
        TipoStatus status = p.getStatus() == null ? TipoStatus.AGUARDANDO_TRIAGEM : p.getStatus();
        return switch (status) {
            case AGUARDANDO_TRIAGEM -> Etapa.AGUARDANDO_ACOLHIMENTO;
            case AGUARDANDO_CONSULTA -> p.getConsultorio() == null ? Etapa.AGUARDANDO_CONSULTA : Etapa.CHAMADO_CONSULTORIO;
            case CHAMADO -> p.getConsultorio() == null ? Etapa.CHAMADO_ACOLHIMENTO : Etapa.CHAMADO_CONSULTORIO;
            case FINALIZADO -> Etapa.FINALIZADO;
            case DESISTENCIA -> Etapa.DESISTIU;
        };
    }

    private static void exigirEtapa(Paciente p, String acao, Etapa... aceitas) {
        Etapa atual = etapaDe(p);
        if (!List.of(aceitas).contains(atual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível " + acao + ": o paciente está " + atual.descricao + ". Atualize a tela.");
        }
    }

    @Transactional
    public Paciente chamarParaTriagem(Long id) {
        Paciente p = buscarPorId(id);
        exigirEtapa(p, "chamar para o acolhimento", Etapa.AGUARDANDO_ACOLHIMENTO, Etapa.CHAMADO_ACOLHIMENTO);

        p.setStatus(TipoStatus.CHAMADO);

        if (p.getChamadaTriagemAt() == null) {
            p.setChamadaTriagemAt(LocalDateTime.now(clock));
        }

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaTriagem(salvo);

        return salvo;
    }

    @Transactional
    public Paciente chamarParaConsultorio(Long id, Long consultorioId) {
        Consultorio c = consultorioRepository.findById(consultorioId)
                .orElseThrow(() -> new RuntimeException("Consultório não encontrado"));
        exigirEtapaParaChamarAoConsultorio(buscarPorId(id));

        // Dois médicos podem clicar quase juntos: só o primeiro reserva o paciente; o segundo recebe 409.
        if (pacienteRepository.reservarParaConsultorio(id, c) == 0) {
            Paciente atual = buscarPorId(id);
            String quem = atual.getConsultorio() != null && atual.getConsultorio().getNumero() != null
                    ? "pelo Consultório " + atual.getConsultorio().getNumero()
                    : "por outro consultório";
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paciente já foi chamado " + quem);
        }

        Paciente p = buscarPorId(id);
        p.setConsultorio(c);
        p.setStatus(TipoStatus.CHAMADO);

        if (p.getChamadaConsultorioAt() == null) {
            p.setChamadaConsultorioAt(LocalDateTime.now(clock));
        }

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaConsultorio(salvo, c);

        return salvo;
    }

    // Já chamado por um consultório mantém a mensagem de qual consultório chamou (vinda da reserva).
    private static void exigirEtapaParaChamarAoConsultorio(Paciente p) {
        Etapa atual = etapaDe(p);
        if (atual != Etapa.CHAMADO_CONSULTORIO) {
            exigirEtapa(p, "chamar ao consultório", Etapa.AGUARDANDO_CONSULTA);
        }
    }

    // A avaliação de ideação/tentativa de suicídio é obrigatória para finalizar. Ela sobrevive à
    // anonimização (que só remove nome, CPF e notas) porque alimenta os indicadores.
    // riscoReclassificado (opcional): a cor do médico vira a cor final do paciente.
    @Transactional
    public void finalizarAtendimento(Long id, ComportamentoSuicida comportamentoSuicida, Risco riscoReclassificado) {
        if (comportamentoSuicida == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "comportamentoSuicida é obrigatório para finalizar a consulta");
        }

        Paciente p = buscarPorId(id);
        exigirEtapa(p, "finalizar o atendimento", Etapa.CHAMADO_CONSULTORIO);
        if (riscoReclassificado != null && riscoReclassificado != p.getRisco()) {
            reclassificarPeloMedico(p, riscoReclassificado);
        }
        p.setStatus(TipoStatus.FINALIZADO);
        p.setAtendimentoFinalizadoAt(LocalDateTime.now(clock));
        p.setComportamentoSuicida(comportamentoSuicida);
        anonimizar(p);
        pacienteRepository.save(p);
        chamadaPainelService.anonimizarChamadasDoPaciente(p.getId(), NOME_ANONIMO);
    }

    // "risco" é a cor final (é ela que os indicadores contam); a da enfermagem fica em riscoTriagem.
    // Só aceita cores que o protocolo define para o tipo do paciente, como na triagem.
    private void reclassificarPeloMedico(Paciente p, Risco novoRisco) {
        boolean existeNoProtocolo = protocoloTempoRepository.findByRiscoAndTipo(novoRisco, p.getTipo())
                .filter(pt -> pt.getTempoMaximoMinutos() != null)
                .isPresent();
        if (!existeNoProtocolo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A cor " + novoRisco + " não existe no protocolo para o atendimento " + p.getTipo());
        }

        if (p.getRiscoTriagem() == null) {
            p.setRiscoTriagem(p.getRisco());
        }
        p.setRisco(novoRisco);
    }

    @Transactional
    public void registrarDesistencia(Long id) {
        Paciente p = buscarPorId(id);
        // As três etapas de status: aguardando acolhimento, chamado (acolhimento ou consultório) e aguardando consulta.
        exigirEtapa(p, "registrar desistência", Etapa.AGUARDANDO_ACOLHIMENTO, Etapa.CHAMADO_ACOLHIMENTO,
                Etapa.AGUARDANDO_CONSULTA, Etapa.CHAMADO_CONSULTORIO);
        p.setEtapaDesistencia(p.getStatus());
        p.setStatus(TipoStatus.DESISTENCIA);
        p.setAtendimentoFinalizadoAt(LocalDateTime.now(clock));
        anonimizar(p);
        pacienteRepository.save(p);
        chamadaPainelService.anonimizarChamadasDoPaciente(p.getId(), NOME_ANONIMO);
    }


    @Transactional
    public Paciente rechamarTriagem(Long id) {
        Paciente p = buscarPorId(id);

        if (p.getChamadaTriagemAt() == null || p.getStatus() != TipoStatus.CHAMADO
                || p.getRisco() != null || p.getConsultorio() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Paciente não está aguardando triagem após chamada");
        }

        p.setRechamadasTriagemCount(
                (p.getRechamadasTriagemCount() == null ? 0 : p.getRechamadasTriagemCount()) + 1);

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaTriagem(salvo);
        return salvo;
    }

    @Transactional
    public Paciente rechamarPaciente(Long id) {
        Paciente p = buscarPorId(id);
        exigirEtapa(p, "rechamar ao consultório", Etapa.CHAMADO_CONSULTORIO);

        // NÃO remove da consulta
        // Apenas dispara novo chamado
        p.setStatus(TipoStatus.CHAMADO);
        p.setRechamadasConsultorioCount(
                (p.getRechamadasConsultorioCount() == null ? 0 : p.getRechamadasConsultorioCount()) + 1);

        Paciente salvo = pacienteRepository.save(p);
        chamadaPainelService.registrarChamadaConsultorio(salvo, salvo.getConsultorio());

        return salvo;
    }


    public List<HistoricoChamadaResponse> listarHistoricoChamadas(Long id) {
        if (!pacienteRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Paciente não encontrado com o ID: " + id);
        }

        return chamadaPainelService.listarHistoricoDoPaciente(id);
    }

    @Transactional
    public Paciente recolocarNaFila(Long id) {
        Paciente p = buscarPorId(id);

        p.setConsultorio(null);
        p.setStatus(TipoStatus.AGUARDANDO_CONSULTA);

        return pacienteRepository.save(p);
    }

    private void anonimizar(Paciente p) {
        p.setNome(NOME_ANONIMO);
        p.setCpf("000.000.000-00");
        p.setTriageNotes(null);
    }
}
