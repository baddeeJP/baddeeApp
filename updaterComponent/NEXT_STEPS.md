# updaterComponent — Next Steps

Pointer for picking up work on the scheduled updating backend. It is a **hub-and-spoke**
updater: one spoke per Japanese data source, downloading → parsing → writing to Postgres
(source of truth) and, for search-relevant data, indexing into Elasticsearch (**write-only**;
a separate app does all querying). See `written_reports/Architectures/Container_Diagram_V03.JPG`.

> **Full handoff plan** (this machine): `~/.claude/plans/check-out-written-reports-architectures-imperative-ocean.md`
> — read its "Current status", "Remaining work", and "Patterns to reuse" sections first.

## Status

- **Done:** hub (scheduler, `SourceVersion` idempotency, hashing, secured `/admin` trigger API),
  **JMdict** spoke (full), **Tatoeba** spoke (full), `docker-compose.yml` (Postgres + Elasticsearch),
  parser unit tests. `./mvnw test` → BUILD SUCCESS.
- **Stubbed:** **Kanji** spoke (combined: KanjiDic2 / KanjiVG / RADKFILE sub-sources) and **JMnedict** spoke.

## Remaining work

1. Implement the **Kanji** sub-sources (`spokes/kanji/{kanjidic2,kanjivg,radkfile}`) — Postgres-only.
2. Implement the **JMnedict** spoke (`spokes/jmnedict`) — Postgres-only.
3. Swap stub JWT auth for **AWS Cognito**: set `updater.admin.stub-auth=false` +
   `spring.security.oauth2.resourceserver.jwt.issuer-uri`, delete `StubJwtDecoderConfig`.
4. Add **integration tests** (Testcontainers) covering persist + index round-trips.

## How to build / run

```bash
docker compose up -d          # Postgres :5432 (updater/updater), Elasticsearch :9200
./mvnw test                   # unit tests (no infra needed)
./mvnw spring-boot:run        # boots; scheduler cron 0 0 3 * * *

# Manual trigger (Bearer token; stub dev token = "dev-token"):
curl -H "Authorization: Bearer dev-token" localhost:8080/admin/updates
curl -H "Authorization: Bearer dev-token" -X POST localhost:8080/admin/updates/jmdict
```

## To finish a new spoke, copy the JMdict/Tatoeba pattern

Sub-packages per spoke: `fetch/` (download + gunzip), `parse/` (streaming parser emitting entities
via a `Consumer`), `domain/` (JPA entity + repository), `search/` (ES doc + write-only writer, JMdict/Tatoeba only),
and a `*Module` at the spoke root implementing `DataSourceModule` (`@Component`, auto-discovered by the scheduler).
Guard work with `Hashing.sha256(file)` + `SourceVersionService.hasChanged(feedId, hash)` / `markUpdated(...)`.
