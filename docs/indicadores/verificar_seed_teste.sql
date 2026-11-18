-- =====================================================================
-- Verificação do seed de docs/indicadores/seed_teste_indicadores.sql
-- Rodar cada SELECT abaixo no pgAdmin (Query Tool, banco `painel`) e
-- comparar com o valor esperado no comentário.
--
-- Estes SELECTs não filtram escopo: somam GERAL + SAMU. O valor esperado
-- de cada indicador por escopo (o que a tela mostra) está em
-- docs/indicadores/gabarito_indicadores.pdf.
-- =====================================================================

-- Esperado: 22
-- (todos os INSERTs do seed, exceto o "Paciente Fora Da Janela" de 25/08)
SELECT COUNT(*) AS cadastrados
FROM paciente
WHERE chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59';

-- Esperado: 20
-- (todos com classified_at preenchido, ou seja, exceto os 2 AGUARDANDO_TRIAGEM)
SELECT COUNT(*) AS classificados
FROM paciente
WHERE classified_at IS NOT NULL
  AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59';

-- Esperado: 17
-- (os 8 pares dentro/fora de protocolo + o reclassificado, todos FINALIZADO)
SELECT COUNT(*) AS atendidos
FROM paciente
WHERE status = 'FINALIZADO'
  AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59';

-- Esperado: NAO_CLASSIFICADO=2, e os demais conforme quantos pacientes
-- de cada cor você inseriu (VERMELHO=5, LARANJA=7, AMARELO=3, VERDE=3, AZUL=2).
-- O reclassificado conta em VERMELHO, a cor do médico (coluna risco).
SELECT risco, COUNT(*) AS total
FROM paciente
WHERE chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
GROUP BY risco
ORDER BY risco;

-- Esperado: 8 linhas (uma por combinação risco/tipo do protocolo_tempo),
-- cada uma com tempo_medio_minutos = média do par dentro/fora que você inseriu.
-- VERMELHO/PSIQUIATRICO inclui o reclassificado: (0 + 5 + 5) / 3 = 3.33
SELECT risco, tipo,
       AVG(EXTRACT(EPOCH FROM (chamada_consultorio_at - classified_at)) / 60.0) AS tempo_medio_minutos
FROM paciente
WHERE classified_at IS NOT NULL
  AND chamada_consultorio_at IS NOT NULL
  AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
GROUP BY risco, tipo
ORDER BY risco, tipo;

-- Esperado: 8 linhas, todas com total=2 e fora_do_prazo=1, exceto
-- VERMELHO/PSIQUIATRICO com total=3 e fora_do_prazo=2 (o reclassificado
-- esperou 5 min: dentro do prazo do LARANJA, fora do VERMELHO)
-- (mesma query nativa usada em IndicadorRepository.contarForaDoProtocoloPorRiscoETipo)
SELECT p.risco, p.tipo,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (p.chamada_consultorio_at - p.classified_at)) / 60.0 > pt.tempo_maximo_minutos) AS fora_do_prazo
FROM paciente p
JOIN protocolo_tempo pt ON pt.risco = p.risco AND pt.tipo = p.tipo
WHERE p.classified_at IS NOT NULL
  AND p.chamada_consultorio_at IS NOT NULL
  AND p.chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
GROUP BY p.risco, p.tipo
ORDER BY p.risco, p.tipo;

-- Esperado: 1 linha, Paciente Teste 21, risco_triagem=LARANJA, risco=VERMELHO,
-- tipo=PSIQUIATRICO, espera_minutos=5 (reclassificado pelo médico ao finalizar)
SELECT nome, risco_triagem, risco, tipo,
       EXTRACT(EPOCH FROM (chamada_consultorio_at - classified_at)) / 60.0 AS espera_minutos
FROM paciente
WHERE risco_triagem IS NOT NULL
  AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59';
