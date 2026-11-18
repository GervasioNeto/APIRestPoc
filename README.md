# APIRestPoc — Painel de Chamadas (backend)

API Spring Boot (Java 21), porta `1111`, persistência em PostgreSQL.

## Banco de dados

A aplicação conecta em `localhost:5432`, banco `painel`, usuário `postgres`.
A senha vem de `DB_PASSWORD`, lida do arquivo `.env` na raiz do projeto (fora do Git):

```sh
cp .env.example .env    # depois preencha DB_PASSWORD
```

### 1. Subir o Postgres

Qualquer uma das opções abaixo serve; o que importa é ter um Postgres respondendo em `localhost:5432`.

- **Instalação local (Windows):** instalar o PostgreSQL 16 (sobe como serviço do Windows) e criar o banco `painel` pelo pgAdmin.
- **Docker:**

  ```sh
  docker run -d --name painel-postgres -e POSTGRES_PASSWORD=<senha> -e POSTGRES_DB=painel -p 5432:5432 postgres:16
  ```

  A `<senha>` precisa ser a mesma do `DB_PASSWORD` no `.env`.

### 2. Criar as tabelas — `docs/indicadores/schema.sql`

Rodar **com o banco vazio, antes de subir a aplicação pela primeira vez**. O `ddl-auto=update` da
aplicação cria as tabelas sozinho no boot, e o script usa `CREATE TABLE` sem `IF NOT EXISTS`
(daria erro de "already exists"). O script também popula `protocolo_tempo` (prazos do Protocolo de
Manchester), que a aplicação não cria.

- **pgAdmin:** Query Tool no banco `painel` → abrir o arquivo → executar.
- **psql:** `psql -U postgres -d painel -f docs/indicadores/schema.sql`
  (no Windows, defina antes `PGCLIENTENCODING=UTF8` para não corromper os acentos).
- **Docker:**

  ```sh
  docker cp docs/indicadores/schema.sql painel-postgres:/tmp/schema.sql
  docker exec painel-postgres psql -U postgres -d painel -f /tmp/schema.sql
  ```

### 3. (Opcional, só em dev) Seed — `docs/indicadores/seed_teste_indicadores.sql`

Insere 23 pacientes fake para testar os indicadores. Rodar da mesma forma que o `schema.sql`, **uma vez só**
(o script não é idempotente: rodar de novo duplica os pacientes). Nunca rodar no banco do hospital.

> ⚠️ **Os dados do seed estão em 01–03/09/2026.** Filtrando por "hoje" (ou qualquer período fora
> dessa janela), o `GET /indicadores` volta tudo zerado — não é bug. Use:
>
> ```http
> GET /indicadores?inicio=2026-09-01T00:00:00&fim=2026-09-03T23:59:59
> ```
>
> Há também 1 paciente de controle em 25/08/2026, fora da janela de propósito.

Esperado nessa janela: escopo `GERAL` (padrão) 20 cadastrados / 18 classificados / 15 atendidos;
escopo `SAMU` 2 / 2 / 2. O valor esperado de cada um dos 27 indicadores, nos dois escopos, está em
[`docs/indicadores/gabarito_indicadores.pdf`](docs/indicadores/gabarito_indicadores.pdf).
Os totais em `docs/indicadores/verificar_seed_teste.sql` (22 / 20 / 17) são sem filtro de escopo.

## Rodando

```sh
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

## Testes

```sh
./mvnw test
```

- Os testes usam o banco `painel_test`, criado automaticamente no mesmo Postgres de `localhost:5432`.
  Nenhum teste toca no banco `painel`.
- `IndicadorRepositoryIntegrationTests` e `IndicadorServiceIntegrationTests` sobem um Postgres
  temporário via Testcontainers, então **precisam do Docker rodando**. Sem Docker, rode o resto assim:

  ```sh
  ./mvnw test '-Dtest=!IndicadorRepositoryIntegrationTests,!IndicadorServiceIntegrationTests'
  ```
