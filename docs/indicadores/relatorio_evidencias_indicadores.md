# Evidências e Relatório de Conclusão: Indicadores Hospitalares

Este documento consolida a execução e os resultados das duas tarefas solicitadas para o backend do Painel de Chamadas do Hospital de Saúde Mental de Fortaleza:
1. **TASK 1**: Validação manual dos indicadores contra dados populados (`painel`).
2. **TASK 2**: Testes automatizados de integração do `IndicadorRepository` contra banco PostgreSQL real (`painel_test`).

---

## TASK 1: Validação Manual dos Indicadores

### 1.1 Preparação e Carga dos Dados de Teste

- **Protocolo de Tempo**: Foram inseridas as 8 regras hospitalares na tabela `protocolo_tempo` conforme definido em [docs/indicadores/schema.sql](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/docs/indicadores/schema.sql).
- **Massa de Dados (Seed)**: A tabela `paciente` foi limpa e populada utilizando [docs/indicadores/seed_teste_indicadores.sql](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/docs/indicadores/seed_teste_indicadores.sql).
- **Janela de Avaliação**: `2026-09-01 00:00:00` até `2026-09-03 23:59:59`.

---

### 1.2 Grupo 1: Indicadores de Contagem e Distribuição de Risco

#### Consulta Executada:
```sql
SELECT 'TOTAL CADASTRADOS (NA JANELA)' AS indicador, COUNT(*) AS valor 
FROM paciente WHERE chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
UNION ALL
SELECT 'TOTAL GERAL NO BANCO (COM FORA DA JANELA)', COUNT(*) FROM paciente
UNION ALL
SELECT 'TOTAL CLASSIFICADOS (NA JANELA)', COUNT(*) 
FROM paciente WHERE classified_at IS NOT NULL AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
UNION ALL
SELECT 'TOTAL ATENDIDOS (NA JANELA)', COUNT(*) 
FROM paciente WHERE status = 'FINALIZADO' AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59';
```

#### Resultado Obtido:
| Indicador | Esperado | Retornado | Status |
| :--- | :---: | :---: | :---: |
| **Cadastrados na janela** | `21` | `21` | ✅ Validado |
| **Total no banco (com controle de 25/08)** | `22` | `22` | ✅ Validado (exclusão de período confirmada) |
| **Classificados na janela** | `19` | `19` | ✅ Validado (2 em triagem sem classificação) |
| **Atendidos na janela** | `16` | `16` | ✅ Validado (`status = 'FINALIZADO'`) |

#### Validação da Inconsistência Documental em `verificar_seed_teste.sql`:
Consulta executada para distribuição por risco:
```sql
SELECT risco, COUNT(*) AS total
FROM paciente
WHERE chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
GROUP BY risco
ORDER BY risco;
```

**Resultado Retornado pelo PostgreSQL**:
- `AMARELO`: 3 (Teste 09, 10, 19)
- `AZUL`: 2 (Teste 13, 14)
- `LARANJA`: **7** (Teste 03, 04, 07, 08, 15, 16 e Paciente Anônimo com DESISTENCIA)
- `NAO_CLASSIFICADO`: 2 (Teste 17, 18)
- `VERDE`: 3 (Teste 11, 12, 20)
- `VERMELHO`: 4 (Teste 01, 02, 05, 06)
- **Soma**: `3 + 2 + 7 + 2 + 3 + 4 = 21`

> [!NOTE]
> **Confirmação da Inconsistência**: O comentário em `verificar_seed_teste.sql` afirmava `LARANJA = 6`, omitindo o registro do `Paciente Anônimo` (risco `LARANJA`, `DESISTENCIA`). O valor real inserido e contabilizado é **7**, totalizando exatamente os 21 registros cadastrados.

---

### 1.3 Grupo 2: Indicadores de Tempo Médio de Espera

Consulta executada (equivalente à query nativa do [IndicadorRepository.java](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/src/main/java/com/example/painel/repository/IndicadorRepository.java#L35-L43)):
```sql
SELECT risco, tipo,
       ROUND(AVG(EXTRACT(EPOCH FROM (chamada_consultorio_at - classified_at)) / 60.0)::numeric, 2) AS tempo_medio_minutos,
       COUNT(*) AS qtd_pacientes
FROM paciente
WHERE classified_at IS NOT NULL
  AND chamada_consultorio_at IS NOT NULL
  AND chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
GROUP BY risco, tipo
ORDER BY risco, tipo;
```

#### Amostra Validada:
1. **LARANJA + CLINICO**:
   - Paciente Teste 03: 09:05 até 09:10 = 5 min
   - Paciente Teste 04: 09:20 até 09:40 = 20 min
   - Média esperada: `(5 + 20) / 2 = 12,50 min`
   - Retornado: **12.50 min** (✅ Bateu)
2. **AMARELO + PSIQUIATRICO**:
   - Paciente Teste 09: 12:05 até 12:35 = 30 min
   - Paciente Teste 10: 12:20 até 13:50 = 90 min
   - Média esperada: `(30 + 90) / 2 = 60,00 min`
   - Retornado: **60.00 min** (✅ Bateu)
3. **AZUL + PSIQUIATRICO**:
   - Paciente Teste 13: 07:05 até 09:05 = 120 min
   - Paciente Teste 14: 07:20 até 12:20 = 300 min
   - Média esperada: `(120 + 300) / 2 = 210,00 min`
   - Retornado: **210.00 min** (✅ Bateu)

---

### 1.4 Grupo 3: Pacientes Fora do Protocolo e Percentual

Consulta executada (equivalente à query nativa do [IndicadorRepository.java](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/src/main/java/com/example/painel/repository/IndicadorRepository.java#L47-L57)):
```sql
SELECT p.risco, p.tipo,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (p.chamada_consultorio_at - p.classified_at)) / 60.0 > pt.tempo_maximo_minutos) AS fora_do_prazo,
       ROUND(
           (COUNT(*) FILTER (WHERE EXTRACT(EPOCH FROM (p.chamada_consultorio_at - p.classified_at)) / 60.0 > pt.tempo_maximo_minutos)::numeric
            / COUNT(*)::numeric) * 100.0, 2
       ) AS perc_fora_do_prazo
FROM paciente p
JOIN protocolo_tempo pt ON pt.risco = p.risco AND pt.tipo = p.tipo
WHERE p.classified_at IS NOT NULL
  AND p.chamada_consultorio_at IS NOT NULL
  AND p.chegada_at BETWEEN '2026-09-01 00:00:00' AND '2026-09-03 23:59:59'
GROUP BY p.risco, p.tipo
ORDER BY p.risco, p.tipo;
```

#### Amostra Validada:
1. **LARANJA + CLINICO** (limite: 10 min):
   - Paciente Teste 03: espera 5 min (dentro)
   - Paciente Teste 04: espera 20 min (fora)
   - Esperado: `total = 2`, `fora_do_prazo = 1`, `percentual = 50.00%`
   - Retornado: `total = 2`, `fora_do_prazo = 1`, `perc_fora_do_prazo = 50.00%` (✅ Bateu)
2. **AMARELO + PSIQUIATRICO** (limite: 60 min):
   - Paciente Teste 09: espera 30 min (dentro)
   - Paciente Teste 10: espera 90 min (fora)
   - Esperado: `total = 2`, `fora_do_prazo = 1`, `percentual = 50.00%`
   - Retornado: `total = 2`, `fora_do_prazo = 1`, `perc_fora_do_prazo = 50.00%` (✅ Bateu)
3. **AZUL + PSIQUIATRICO** (limite: 240 min):
   - Paciente Teste 13: espera 120 min (dentro)
   - Paciente Teste 14: espera 300 min (fora)
   - Esperado: `total = 2`, `fora_do_prazo = 1`, `percentual = 50.00%`
   - Retornado: `total = 2`, `fora_do_prazo = 1`, `perc_fora_do_prazo = 50.00%` (✅ Bateu)

> [!TIP]
> **Onde calcular o percentual**: O `IndicadorRepository` retorna a projeção `ForaDoProtocoloPorRiscoTipo` com `total` e `foraDoPrazo`. O cálculo do percentual (`(fora / total) * 100`) deve ser efetuado na camada de **Service / DTO**, prevenindo divisão por zero (`total == 0 ? 0.0 : ...`) e permitindo formatações sem acoplar regras de apresentação ao SQL nativo.

---

## TASK 2: Testes Automatizados de Integração no `IndicadorRepository`

### 2.1 Isolamento do Banco de Testes

- **Banco dedicado**: Criado o banco `painel_test` no PostgreSQL.
- **Configuração de Teste**: Criado [src/test/resources/application-test.properties](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/src/test/resources/application-test.properties) com:
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/painel_test`
  - `spring.datasource.password=${DB_PASSWORD}`
  - `spring.jpa.hibernate.ddl-auto=create-drop`
- **Arquivo de Variáveis**: Arquivo [.env](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/.env) configurado na raiz (sem BOM UTF-8) com `DB_PASSWORD=0774` (ignorado pelo Git).

---

### 2.2 Implementação do Teste de Integração

Criada a classe [src/test/java/com/example/painel/repository/IndicadorRepositoryIntegrationTests.java](file:///c:/Users/Notebook%20JP/Desktop/backpainel/APIRestPoc/src/test/java/com/example/painel/repository/IndicadorRepositoryIntegrationTests.java):
- Utiliza `@DataJpaTest`, `@ActiveProfiles("test")` e `@AutoConfigureTestDatabase(replace = NONE)`.
- Contém 3 testes de integração com queries reais no PostgreSQL:
  1. `deveCalcularContagensGeraisCorretamenteExcluindoForaDoPeriodo`: valida `countCadastrados`, `countClassificados`, `countAtendidos` e a exclusão estrita do registro fora do intervalo.
  2. `deveCalcularTempoMedioDeEsperaPorRiscoETipo`: valida o cálculo de média nativo (`AVG` com `EXTRACT(EPOCH)`) resultando em `12.5` minutos.
  3. `deveCalcularPacientesForaDoProtocoloEPercentual`: cadastra o protocolo de 10 min, insere pacientes dentro e fora do prazo, valida o retorno do repositório (`total = 2`, `foraDoPrazo = 1`) e calcula o percentual de 50%.

---

### 2.3 Resultado da Execução dos Testes Automatizados

Comando executado via PowerShell:
```powershell
.\mvnw.cmd test -Dtest=IndicadorRepositoryIntegrationTests
```

**Saída**:
```text
[INFO] Running com.example.painel.repository.IndicadorRepositoryIntegrationTests
Hibernate: create table chamada_painel (...)
Hibernate: create table consultorio (...)
Hibernate: create table paciente (...)
Hibernate: create table protocolo_tempo (...)
Hibernate: create table triagem (...)
...
Hibernate: insert into paciente (...) values (...)
Hibernate: SELECT risco, tipo, AVG(EXTRACT(EPOCH FROM (chamada_consultorio_at - classified_at)) / 60.0) AS tempo_medio_minutos ...
Hibernate: insert into protocolo_tempo (...) values (...)
Hibernate: SELECT p.risco AS risco, p.tipo AS tipo, COUNT(*) AS total, COUNT(*) FILTER (...) AS fora_do_prazo ...
Hibernate: select count(p1_0.id) from paciente p1_0 where p1_0.chegada_at between ? and ?
Hibernate: select count(p1_0.id) from paciente p1_0 where p1_0.classified_at is not null and p1_0.chegada_at between ? and ?
Hibernate: select count(p1_0.id) from paciente p1_0 where p1_0.status='FINALIZADO' and p1_0.chegada_at between ? and ?
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.53 s -- in com.example.painel.repository.IndicadorRepositoryIntegrationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
