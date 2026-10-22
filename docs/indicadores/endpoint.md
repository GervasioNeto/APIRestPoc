# GET /indicadores

documentação de como consultar e interpretar o GET /indicadores

## Parâmetros

- `inicio` e `fim`: obrigatórios, data/hora ISO local sem fuso, por exemplo
  `2026-09-01T00:00:00`. Não converter para UTC.
- `escopo`: opcional, `GERAL` (padrão) ou `SAMU`, em maiúsculas.
- `GERAL` exclui `tipo = SAMU`, mas inclui `tipo IS NULL`.
- `SAMU` inclui somente `tipo = SAMU`.
- O período filtra `chegadaAt` com `BETWEEN`: início e fim inclusivos.
  Datas iguais são válidas. Para cobrir frações do último segundo do dia,
  pode-se usar `2026-09-03T23:59:59.999999999`.

```http
GET /indicadores?inicio=2026-09-01T00:00:00&fim=2026-09-03T23:59:59&escopo=GERAL
GET /indicadores?inicio=2026-09-01T00:00:00&fim=2026-09-03T23:59:59&escopo=SAMU
```

HTTP 200 para consultas válidas, inclusive sem registros. HTTP 400 para escopo
inválido, datas malformadas, datas ausentes ou início posterior ao fim.
Erros internos de banco não são convertidos em erros de validação.

## Contrato preservado

O JSON continua sendo `IndicadoresResponse`, sem novos campos. Mantém as 15
combinações de risco/tipo no detalhamento; combinações excluídas pelo escopo têm
contagens e percentuais zero e média null. O filtro acontece nas seis queries,
antes das agregações; não se carregam pacientes para filtrar em memória.

As regras de status continuam individuais: cadastrados não filtra status;
classificados exige `classifiedAt`; atendidos exige `FINALIZADO`; contagens por
risco não filtram status; médias e protocolo exigem classificação e chamada ao
consultório. Não foi adicionado filtro global de atendimentos encerrados.

Percentuais de classificados e atendidos usam cadastrados do mesmo escopo.
Percentual fora do protocolo usa o total da mesma combinação risco/tipo, com
protocolo cadastrado e as duas datas preenchidas. Percentuais estão em 0-100,
com duas casas, HALF_UP. Denominador zero retorna 0.00; médias sem amostras são
null e tempos são em minutos.

**Mudança de comportamento:** consulta sem escopo agora significa GERAL e
exclui SAMU. Antes, os totais misturavam todos os tipos. O seed e o script SQL
histórico de verificação continuam intactos e representam os dados sem filtro;
seus totais combinados não são a expectativa do novo padrão GERAL.
Indicadores 1-3 continuam pendentes e não foram implementados.

## Aceite com seed original

Período de 2026-09-01T00:00:00 a 2026-09-03T23:59:59:

| Campo | GERAL | SAMU |
|---|---:|---:|
| cadastrados | 19 | 2 |
| classificados | 17 | 2 |
| atendidos | 14 | 2 |
| percentualClassificados | 89.47 | 100.00 |
| percentualAtendidos | 73.68 | 100.00 |
| contagensPorRisco.LARANJA | 5 | 2 |

SAMU/LARANJA: média 12.5 minutos, totalComProtocolo=2, foraDoPrazo=1,
percentualForaDoPrazo=50.00. Em GERAL, os grupos SAMU ficam vazios; no escopo
SAMU, os grupos CLINICO e PSIQUIATRICO ficam vazios.

## Testes isolados

```sh
./mvnw -Dtest=IndicadorServiceTests,IndicadorControllerTests,IndicadorServiceIntegrationTests,IndicadorRepositoryIntegrationTests test
```

Os testes de integração de indicadores usam PostgreSQL 16 temporário via
Testcontainers e sobrescrevem URL, usuário e senha. O teste do service carrega
`schema.sql` e `seed_teste_indicadores.sql` originais e usa MockMvc com service e
repository reais. Não usa nem limpa o banco de desenvolvimento.

## Registro da entrega - 16/10/2026

Implementado filtro GERAL/SAMU no controller, service e nas seis queries de
indicadores, com enum EscopoIndicadores. Preservados DTO, seed, limites de
data e regras individuais de status. Validação: 21 testes aprovados, incluindo
PostgreSQL isolado via Testcontainers, service real e endpoint via MockMvc.

O frontend possui uma tela provisória para visualizar dados e validar estados,
utilizando componentes existentes do site. Não corresponde ao design oficial.
A integração do frontend foi concluída na branch
`feat/indicadores-dados-cards-provisorios` (commit `7f7dfc4`): hook ativo nos dois
escopos, mocks reais GERAL e SAMU capturados e commitados, datas locais sem UTC,
cards e estados de carregamento/erro/vazio. Foram aprovados 25 testes da feature
(17 locais, 7 HTTP reais com servidor ativo e 1 com servidor desligado), além
do build e lint dos arquivos da feature.

A tela continua provisória, usando componentes existentes. A integração com a
estrutura mais recente da develop do frontend e a adequação ao design oficial
continuam pendentes. Desistências, indicadores 1-3 e ocultação de cards SAMU
(IND-10) não foram implementados nesta entrega.
