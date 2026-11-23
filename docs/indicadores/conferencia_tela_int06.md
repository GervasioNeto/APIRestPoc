# Conferência na tela · INT-06 · Atendidos por Horário

Conferência da aba **Pacientes Atendidos por Horário** contra o [gabarito da INT-00](gabarito_indicadores.pdf)
(linhas H1 e H2) e contra o `GET /indicadores/atendidos`, com `schema.sql` + `seed_teste_indicadores.sql`
num banco limpo, período de 01/09/2026 a 03/09/2026, back `develop` 6ac1113 e front `develop` 42134ba.
Feita em 23/11/2026 lendo os valores exibidos na tela, nos dois escopos.

**Resultado:** todos os valores batem com o gabarito e com o back, incluindo as faixas de turno, os
horários de borda, a faixa que cruza a meia-noite e a faixa sem atendidos. Nenhuma correção no front
nem no back.

## H1 e H2 · sem faixa de horário

| # | Onde na tela | Esperado GERAL | Tela GERAL | Esperado SAMU | Tela SAMU | Status |
|---|---|---:|---:|---:|---:|---|
| H1 | Atendidos por Horário › “Atendidos na faixa” (“de N no período”) | 15 (de 15) | 15 (de 15) | 2 (de 2) | 2 (de 2) | ok |
| H2 | Atendidos por Horário › “Total de rechamadas” | 7 | 7 | 2 | 2 | ok |

## Lista de atendidos · sem faixa de horário

O horário considerado é o da 1ª chamada ao consultório (`chamadaConsultorioAt`).

| Conferência | Esperado GERAL | Esperado SAMU | Status |
|---|---|---|---|
| Cores na lista | Vermelho 5 · Laranja 4 · Amarelo 2 · Verde 2 · Azul 2 | Laranja 2 | ok |
| Datas e horários | 01/09 08:05 → 03/09 09:10 (15 linhas) | 02/09 08:10 e 08:40 | ok |
| Rechamadas por paciente | Teste 01: 3× · Teste 02: 1× · Teste 03: 1× · Teste 09: 2× · demais 0× | Teste 15: 2× · Teste 16: 0× | ok |

Laranja atendido no GERAL é 4, não 5 (indicador 11): o 5º Laranja é o Paciente Anônimo, que desistiu e
não foi chamado ao consultório. O Paciente Teste 21 (reclassificado Laranja → Vermelho) aparece em
Vermelho.

## Faixas de horário

| Faixa | Esperado GERAL | Esperado SAMU | Status |
|---|---|---|---|
| Madrugada 00:00–05:59 | 0 · “Nenhum atendido nesta faixa de horário” | 0 · mesma mensagem | ok |
| Manhã 06:00–11:59 | 10 · 5 rechamadas · Vermelho 5, Laranja 4, Azul 1 | 2 · 2 rechamadas | ok |
| Tarde 12:00–17:59 | 5 · 2 rechamadas · Amarelo 2, Verde 2, Azul 1 | 0 · mensagem de vazio | ok |
| Noite 18:00–23:59 | 0 · mensagem de vazio | 0 · mensagem de vazio | ok |

Em todas as faixas o subtítulo continua “de 15 no período” (GERAL) / “de 2 no período” (SAMU).

## Bordas, meia-noite e fuso (GERAL)

| Faixa digitada | Esperado | Tela | Status |
|---|---|---|---|
| 07:59 → 07:59 | 0 | 0 | ok |
| 08:00 → 08:00 | 0 | 0 | ok |
| 08:05 → 08:05 | 1 (Teste 01) | 1 | ok |
| 08:05 → 08:20 | 2 (Testes 01 e 02) | 2 | ok |
| 09:05 → 09:10 | 3 (Testes 13, 03 e 21; a faixa ignora o dia) | 3 | ok |
| 22:00 → 06:00 (cruza a meia-noite) | 0 | 0 | ok |
| Início 15:00, sem fim | 1 (Teste 12, 15:50) | 1 | ok |

- **Faixa inclusiva nas duas pontas:** 08:05 → 08:05 traz o paciente chamado às 08:05. **ok**
- **Fuso:** a hora na tela é a mesma gravada no banco (ex.: 01/09 08:05), sem deslocamento. O front
  lê o `LocalDateTime` como texto (`horaDe`), sem passar por `Date`. **ok**

## Observações

- O seed não tem chamada ao consultório exatamente às 07:59 ou 08:00 (a primeira é 08:05). Por isso
  essas duas bordas só confirmam o vazio; a inclusão da ponta foi conferida com 08:05.
- Os campos Hora Início / Hora Fim aparecem em AM/PM em navegadores com idioma ou região em inglês
  (o `input type="time"` segue o sistema). A frase da faixa e a lista continuam em 24h. Não afeta os
  valores.
