# updaterComponent — Next Steps

Pointer for picking up work on the scheduled updating backend. It is a **hub-and-spoke**
updater: one spoke per Japanese data source, downloading → parsing → writing to Postgres
(source of truth) and, for search-relevant data, indexing into Elasticsearch (**write-only**;
a separate app does all querying). See `written_reports/Architectures/Container_Diagram_V03.JPG`.

## Status

- **Done:** hub (scheduler, `SourceVersion` idempotency, hashing, secured `/admin` trigger API),
  all four spokes — **JMdict** and **Tatoeba** (Postgres + ES), **JMnedict** and the combined
  **Kanji** spoke (KanjiDic2 / RADKFILE / KanjiVG sub-sources; Postgres only).
- **Auth:** Cognito-ready. `CognitoJwtDecoderConfig` validates issuer, expiry, `token_use=access`
  and an allow-listed `client_id`; `/admin/**` also requires the `updater.admin.required-group`
  Cognito group (default `updater-admin`, 403 otherwise). The stub dev token stays the default
  for local development.
- **Tests:** parser + auth unit tests (`./mvnw test`, no infra) and Testcontainers integration
  tests (`./mvnw verify`, needs Docker) covering every spoke's fetch → persist → index round-trip,
  hash-based skip, changed-file updates, Kanji sub-source order independence, and admin API auth.
- **Verified on real upstream data** (2026-09-27, compose stack): JMdict 218,830 entries (~8.5 min),
  JMnedict 743,670 names, KanjiDic2 13,108 kanji, RADKFILE 253 radicals / 6,355 kanji,
  KanjiVG 6,446 kanji with strokes, Tatoeba 147,836 sentences; ES doc counts match Postgres.
  Unchanged feeds are skipped on re-run.

## Remaining work

1. **Go live on Cognito** (needs a real user pool): create the `updater-admin` group and an app
   client, then set `updater.admin.stub-auth=false`,
   `spring.security.oauth2.resourceserver.jwt.issuer-uri` and `updater.admin.cognito.client-ids`.
   Delete `StubJwtDecoderConfig` once no environment uses it.
2. **Schema management:** `ddl-auto=update` can't change column types. Consider Flyway before
   prod. Any DB created before the 2026-09-27 fixes (text columns, Tatoeba re-keyed on the
   Japanese id) should be reset: `docker compose down -v`.
3. **Throughput (optional):** JMdict child rows use `IDENTITY` ids, which disables JDBC batching,
   and `saveAll` on existing ids merges row-by-row. Fine for a nightly job; switch to
   `SEQUENCE` ids / bulk upserts if run time matters.
4. **Removed upstream entries** are never deleted (only upserted). Decide whether that matters.

## How to build / run

```bash
docker compose up -d          # Postgres :5432 (updater/updater), Elasticsearch 9 :9200
./mvnw test                   # unit tests (no infra needed)
./mvnw verify                 # + integration tests (Testcontainers; Docker required)
./mvnw spring-boot:run        # boots; scheduler cron 0 0 3 * * *

# Manual trigger (Bearer token; stub dev token = "dev-token"):
curl -H "Authorization: Bearer dev-token" localhost:8080/admin/updates
curl -H "Authorization: Bearer dev-token" -X POST localhost:8080/admin/updates/jmdict
```

Elasticsearch's major version must match the `elasticsearch-java` client Spring Boot manages
(9.x for Boot 4.1): an 8.x server rejects the 9.x client.

## To add a new spoke, copy the JMdict/Tatoeba pattern

Sub-packages per spoke: `fetch/` (download + gunzip), `parse/` (streaming parser emitting entities
via a `Consumer`), `domain/` (JPA entity + repository), `search/` (ES doc + write-only writer,
JMdict/Tatoeba only), and a `*Module` at the spoke root implementing `DataSourceModule`
(`@Component`, auto-discovered by the scheduler).
Guard work with `Hashing.sha256(file)` + `SourceVersionService.hasChanged(feedId, hash)` / `markUpdated(...)`.
Gotchas found the hard way: Spring Data ignores *nested* repository interfaces; `@Lob String`
becomes an `oid` on Postgres (use `columnDefinition = "text"`); default `varchar(255)` (and
`varchar(255)[]` for list columns) is too short for some real values, so use `text` / `text[]`;
don't call the unauthenticated GitHub API from a scheduled job (60 req/h per shared IP; KanjiVG
uses the `releases/latest` web redirect instead). Add a `*ModuleIT` extending
`support.IntegrationTest`, and do one real-data run: fixtures alone missed most of these.
