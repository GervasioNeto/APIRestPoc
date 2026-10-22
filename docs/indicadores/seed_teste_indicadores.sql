-- =====================================================================
-- Seed de teste para o IndicadorRepository
--
-- Popula `paciente` com dados fake cobrindo os cenários que as queries
-- de docs/indicadores precisam exercitar. Rodar manualmente no pgAdmin
-- (Query Tool, banco `painel`) -- não é executado pelo programa.
--
-- Janela de teste: chegada_at entre 2026-09-01 00:00 e 2026-09-03 23:59.
-- Um paciente fica de propósito FORA dessa janela (25/08) pra confirmar
-- que o filtro de período dos indicadores exclui ele.
--
-- Resultado esperado, filtrando inicio=2026-09-01 00:00 fim=2026-09-03 23:59:
--   countCadastrados      = 21  (todos os INSERTs abaixo, exceto o de 25/08)
--   countClassificados    = 19  (todos com classified_at, ou seja, exceto os 2 AGUARDANDO_TRIAGEM)
--   countAtendidos         = 16  (os 8 pares dentro/fora de protocolo, status FINALIZADO)
--   contarForaDoProtocoloPorRiscoETipo -> pra cada uma das 8 combinações
--     risco/tipo de protocolo_tempo, total=2 e fora_do_prazo=1 (o segundo de cada par)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Pares "dentro do protocolo" / "fora do protocolo", um por combinação
-- de protocolo_tempo. Todos FINALIZADOS.
-- ---------------------------------------------------------------------

-- VERMELHO / CLINICO (máximo 0 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 01', '000.000.000-01', 'FINALIZADO', 'VERMELHO', 'CLINICO',
 TIMESTAMP '2026-09-01 08:00:00', TIMESTAMP '2026-09-01 08:02:00', TIMESTAMP '2026-09-01 08:05:00',
 TIMESTAMP '2026-09-01 08:05:00', TIMESTAMP '2026-09-01 08:25:00'),
('Paciente Teste 02', '000.000.000-02', 'FINALIZADO', 'VERMELHO', 'CLINICO',
 TIMESTAMP '2026-09-01 08:10:00', TIMESTAMP '2026-09-01 08:12:00', TIMESTAMP '2026-09-01 08:15:00',
 TIMESTAMP '2026-09-01 08:20:00', TIMESTAMP '2026-09-01 08:40:00');

-- LARANJA / CLINICO (máximo 10 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 03', '000.000.000-03', 'FINALIZADO', 'LARANJA', 'CLINICO',
 TIMESTAMP '2026-09-01 09:00:00', TIMESTAMP '2026-09-01 09:02:00', TIMESTAMP '2026-09-01 09:05:00',
 TIMESTAMP '2026-09-01 09:10:00', TIMESTAMP '2026-09-01 09:30:00'),
('Paciente Teste 04', '000.000.000-04', 'FINALIZADO', 'LARANJA', 'CLINICO',
 TIMESTAMP '2026-09-01 09:15:00', TIMESTAMP '2026-09-01 09:17:00', TIMESTAMP '2026-09-01 09:20:00',
 TIMESTAMP '2026-09-01 09:40:00', TIMESTAMP '2026-09-01 10:00:00');

-- VERMELHO / PSIQUIATRICO (máximo 0 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 05', '000.000.000-05', 'FINALIZADO', 'VERMELHO', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 10:00:00', TIMESTAMP '2026-09-01 10:02:00', TIMESTAMP '2026-09-01 10:05:00',
 TIMESTAMP '2026-09-01 10:05:00', TIMESTAMP '2026-09-01 10:25:00'),
('Paciente Teste 06', '000.000.000-06', 'FINALIZADO', 'VERMELHO', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 10:15:00', TIMESTAMP '2026-09-01 10:17:00', TIMESTAMP '2026-09-01 10:20:00',
 TIMESTAMP '2026-09-01 10:25:00', TIMESTAMP '2026-09-01 10:45:00');

-- LARANJA / PSIQUIATRICO (máximo 10 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 07', '000.000.000-07', 'FINALIZADO', 'LARANJA', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 11:00:00', TIMESTAMP '2026-09-01 11:02:00', TIMESTAMP '2026-09-01 11:05:00',
 TIMESTAMP '2026-09-01 11:10:00', TIMESTAMP '2026-09-01 11:30:00'),
('Paciente Teste 08', '000.000.000-08', 'FINALIZADO', 'LARANJA', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 11:15:00', TIMESTAMP '2026-09-01 11:17:00', TIMESTAMP '2026-09-01 11:20:00',
 TIMESTAMP '2026-09-01 11:35:00', TIMESTAMP '2026-09-01 11:55:00');

-- AMARELO / PSIQUIATRICO (máximo 60 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 09', '000.000.000-09', 'FINALIZADO', 'AMARELO', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 12:00:00', TIMESTAMP '2026-09-01 12:02:00', TIMESTAMP '2026-09-01 12:05:00',
 TIMESTAMP '2026-09-01 12:35:00', TIMESTAMP '2026-09-01 13:00:00'),
('Paciente Teste 10', '000.000.000-10', 'FINALIZADO', 'AMARELO', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 12:15:00', TIMESTAMP '2026-09-01 12:17:00', TIMESTAMP '2026-09-01 12:20:00',
 TIMESTAMP '2026-09-01 13:50:00', TIMESTAMP '2026-09-01 14:10:00');

-- VERDE / PSIQUIATRICO (máximo 120 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 11', '000.000.000-11', 'FINALIZADO', 'VERDE', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 13:00:00', TIMESTAMP '2026-09-01 13:02:00', TIMESTAMP '2026-09-01 13:05:00',
 TIMESTAMP '2026-09-01 14:05:00', TIMESTAMP '2026-09-01 14:30:00'),
('Paciente Teste 12', '000.000.000-12', 'FINALIZADO', 'VERDE', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 13:15:00', TIMESTAMP '2026-09-01 13:17:00', TIMESTAMP '2026-09-01 13:20:00',
 TIMESTAMP '2026-09-01 15:50:00', TIMESTAMP '2026-09-01 16:10:00');

-- AZUL / PSIQUIATRICO (máximo 240 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 13', '000.000.000-13', 'FINALIZADO', 'AZUL', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 07:00:00', TIMESTAMP '2026-09-01 07:02:00', TIMESTAMP '2026-09-01 07:05:00',
 TIMESTAMP '2026-09-01 09:05:00', TIMESTAMP '2026-09-01 09:30:00'),
('Paciente Teste 14', '000.000.000-14', 'FINALIZADO', 'AZUL', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-01 07:15:00', TIMESTAMP '2026-09-01 07:17:00', TIMESTAMP '2026-09-01 07:20:00',
 TIMESTAMP '2026-09-01 12:20:00', TIMESTAMP '2026-09-01 12:45:00');

-- LARANJA / SAMU (máximo 10 min)
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Teste 15', '000.000.000-15', 'FINALIZADO', 'LARANJA', 'SAMU',
 TIMESTAMP '2026-09-02 08:00:00', TIMESTAMP '2026-09-02 08:02:00', TIMESTAMP '2026-09-02 08:05:00',
 TIMESTAMP '2026-09-02 08:10:00', TIMESTAMP '2026-09-02 08:30:00'),
('Paciente Teste 16', '000.000.000-16', 'FINALIZADO', 'LARANJA', 'SAMU',
 TIMESTAMP '2026-09-02 08:15:00', TIMESTAMP '2026-09-02 08:17:00', TIMESTAMP '2026-09-02 08:20:00',
 TIMESTAMP '2026-09-02 08:40:00', TIMESTAMP '2026-09-02 09:00:00');

-- ---------------------------------------------------------------------
-- Pacientes ainda em andamento, pra distinguir os 3 contadores base
-- (cadastrados / classificados / atendidos) entre si.
-- ---------------------------------------------------------------------

-- Ainda aguardando triagem: entram em countCadastrados, mas NÃO em
-- countClassificados (classified_at nulo) nem countAtendidos.
INSERT INTO paciente (nome, cpf, status, risco, chegada_at) VALUES
('Paciente Teste 17', '000.000.000-17', 'AGUARDANDO_TRIAGEM', 'NAO_CLASSIFICADO', TIMESTAMP '2026-09-02 15:00:00'),
('Paciente Teste 18', '000.000.000-18', 'AGUARDANDO_TRIAGEM', 'NAO_CLASSIFICADO', TIMESTAMP '2026-09-02 15:10:00');

-- Já classificados, aguardando consultório: entram em countClassificados
-- mas NÃO em countAtendidos (status ainda não é FINALIZADO).
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at) VALUES
('Paciente Teste 19', '000.000.000-19', 'AGUARDANDO_CONSULTA', 'AMARELO', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-02 16:00:00', TIMESTAMP '2026-09-02 16:02:00', TIMESTAMP '2026-09-02 16:05:00'),
('Paciente Teste 20', '000.000.000-20', 'AGUARDANDO_CONSULTA', 'VERDE', 'PSIQUIATRICO',
 TIMESTAMP '2026-09-02 16:10:00', TIMESTAMP '2026-09-02 16:12:00', TIMESTAMP '2026-09-02 16:15:00');

-- Desistência após a classificação (etapa_desistencia registrada).
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, etapa_desistencia) VALUES
('Paciente Anônimo', '000.000.000-00', 'DESISTENCIA', 'LARANJA', 'CLINICO',
 TIMESTAMP '2026-09-02 17:00:00', TIMESTAMP '2026-09-02 17:02:00', TIMESTAMP '2026-09-02 17:05:00', 'AGUARDANDO_CONSULTA');

-- ---------------------------------------------------------------------
-- Controle: fora da janela de teste (25/08), pra confirmar que o filtro
-- de período (chegada_at BETWEEN :inicio AND :fim) exclui esse paciente
-- quando a janela usada for 01/09 a 03/09.
-- ---------------------------------------------------------------------
INSERT INTO paciente (nome, cpf, status, risco, tipo, chegada_at, chamada_triagem_at, classified_at, chamada_consultorio_at, atendimento_finalizado_at) VALUES
('Paciente Fora Da Janela', '000.000.000-99', 'FINALIZADO', 'VERMELHO', 'CLINICO',
 TIMESTAMP '2026-08-25 08:00:00', TIMESTAMP '2026-08-25 08:02:00', TIMESTAMP '2026-08-25 08:05:00',
 TIMESTAMP '2026-08-25 08:05:00', TIMESTAMP '2026-08-25 08:25:00');
