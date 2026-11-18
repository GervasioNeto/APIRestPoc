# Conferência na tela · INT-04 e INT-05

Conferência da tela de Indicadores contra o [gabarito da INT-00](gabarito_indicadores.pdf), com
`schema.sql` + `seed_teste_indicadores.sql` num banco limpo, período de 01/09/2026 a 03/09/2026,
back `develop` a7cba18 e front `feat/int-04-05-conferencia-indicadores` (sobre o `develop` 44a4cc5).
Feita em 18/11/2026 lendo os valores exibidos na tela, nos dois escopos.

**Resultado:** com dados, todos os valores batiam com o gabarito antes da correção. A divergência
estava no caso **sem amostras**: os tempos apareciam `--:--:--` e o % fora do protocolo aparecia
`0%` numa combinação sem ninguém atendido. Os dois agora mostram `—` (front: `formatarMinutos` e
`EmergencyLevelTab`). Nenhuma correção no back.

## INT-04 · Cards do topo e Tempos de Espera (1–3)

| # | Onde na tela | Esperado GERAL | Tela GERAL | Esperado SAMU | Tela SAMU | Status |
|---|---|---:|---:|---:|---:|---|
| 1 | Tempos de Espera › Espera p/ acolhimento / classificação | 00:02:00 | 00:02:00 | 00:04:00 | 00:04:00 | ok |
| 2 | Tempos de Espera › Espera p/ 1º atendimento médico | 00:54:00 | 00:54:00 | 00:12:30 | 00:12:30 | ok |
| 3 | Tempos de Espera › Espera total (NAC → médico) | 00:59:00 | 00:59:00 | 00:17:30 | 00:17:30 | ok |
| 3 | Topo › Tempo Médio de Espera | 00:59:00 | 00:59:00 | 00:17:30 | 00:17:30 | ok |
| 6 | Topo › Total de Atendimentos | 15 | 15 | 2 | 2 | ok |
| — | Sem amostras (período sem atendimentos) | — | — (antes `--:--:--`) | — | — (antes `--:--:--`) | corrigido |

Formatação: minutos em HH:MM:SS arredondados ao segundo (`formatarMinutos`); sem amostras o back
manda `null` e a tela mostra `—`, nunca `00:00:00` (que seria espera zero de verdade).

## INT-05 · Por Nível de Emergência (14–27)

Tempo médio · % fora do protocolo · (n) = `totalComProtocolo`.

| # | Cor (tipo) | Esperado | Tela | Status |
|---|---|---|---|---|
| 15 · 21 | Vermelho (psiquiátrico) | 00:03:20 · 66,67% · (3) | 00:03:20 · 66,67% · (3) | ok |
| 14 · 22 | Vermelho (clínico) | 00:02:30 · 50% · (2) | 00:02:30 · 50% · (2) | ok |
| 17 · 23 | Laranja (psiquiátrico) | 00:10:00 · 50% · (2) | 00:10:00 · 50% · (2) | ok |
| 16 · 24 | Laranja (clínico) | 00:12:30 · 50% · (2) | 00:12:30 · 50% · (2) | ok |
| 18 · 25 | Amarelo (psiquiátrico) | 01:00:00 · 50% · (2) | 01:00:00 · 50% · (2) | ok |
| 19 · 26 | Verde (psiquiátrico) | 01:45:00 · 50% · (2) | 01:45:00 · 50% · (2) | ok |
| 20 · 27 | Azul (psiquiátrico) | 03:30:00 · 50% · (2) | 03:30:00 · 50% · (2) | ok |
| — | Laranja (SAMU), escopo SAMU | 00:12:30 · 50% · (2) | 00:12:30 · 50% · (2) | ok |
| — | Combinação sem atendidos (ex.: 02 a 03/09) | — · — · (0) | — · — · (0) (antes `--:--:--` · `0%`) | corrigido |

- **Reclassificado:** o Paciente Teste 21 (Laranja pela enfermagem, Vermelho pelo médico) aparece
  em Vermelho psiquiátrico (n = 3, 2 fora do prazo). Se contasse como Laranja, seriam 50% (2). **ok**
- **Só combinações do protocolo:** GERAL mostra Psiquiátrico com as 5 cores e Clínico só com
  Vermelho e Laranja; SAMU mostra só Laranja. **ok**

Os valores acima ficam travados no front por `src/features/indicators/gabarito.test.tsx`, que usa
o `GET /indicadores` real deste seed (`mocks/gabarito-int00-geral.json` e `-samu.json`).
