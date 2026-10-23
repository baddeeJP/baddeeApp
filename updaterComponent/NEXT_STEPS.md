# updaterComponent — Next Steps

All four spokes (JMdict, JMnedict, Kanji, Tatoeba) are implemented, covered by unit and
Testcontainers integration tests, and verified end-to-end on real upstream data (2026-09-27).
Full history of that work, build/run commands and hard-won gotchas:
`../written_reports/Archived/Claude/NEXT_STEPS.md`.

## 1. Go live on AWS Cognito

The code is ready (`hub/CognitoJwtDecoderConfig`, `hub/SecurityConfig`); it needs a real user pool.

- [ ] Create (or pick) the user pool, an app client for admin callers, and a Cognito group
      `updater-admin` (or change `updater.admin.required-group`). Add admin users to the group.
- [ ] Configure the deployed environment: the `prod` profile already sets
      `updater.admin.stub-auth=false` and reads `COGNITO_ISSUER_URI`
      (`https://cognito-idp.<region>.amazonaws.com/<userPoolId>`) and `COGNITO_CLIENT_IDS`.
- [ ] Smoke-test with a real **access** token (ID tokens are rejected by design): expect 200 on
      `GET /admin/updates` for a group member, 403 for a non-member, 401 without a token.
- [ ] Once no environment uses the dev token, delete `hub/StubJwtDecoderConfig` and the
      `updater.admin.dev-token` / `stub-auth` properties.

## 2. Schema migrations: done (2026-10-06)

Flyway owns the schema (`src/main/resources/db/migration`); Hibernate runs with
`ddl-auto=validate`, so an entity/schema mismatch fails startup. `V1__baseline.sql` is the
Hibernate-generated schema as of 2026-10-06 (verified identical via `pg_dump` diff).

- Schema changes: add a new `V<n>__<description>.sql`; never edit an applied one.
- Pre-Flyway databases with that exact schema are adopted as V1 by
  `spring.flyway.baseline-on-migrate=true` (the compose DB was, keeping its 2026-09-27 data).
  A DB whose schema differs from V1 (anything created before 2026-09-27) must be reset:
  `docker compose down -v` (from the repo root; this deletes the data volumes).

## 3. Entries removed upstream: done (2026-10-06)

An entry missing from a changed upstream file is deleted from Elasticsearch and **retired** in
Postgres (`retired_at` set, row kept). It is un-retired and re-indexed if it comes back. Kanji
sub-sources clear only their own data, and a kanji is retired once no sub-source has it. The
guard `updater.<feedId>.retire-guard` defaults to 0.98. Policy and reasons:
`docs/adr/0001-retire-in-postgres-delete-from-search.md`.

- [ ] The future API backend must filter `retired_at IS NULL` (a view or a default repository
      filter there).
- [ ] Tune each feed's `retire-guard` from the logged `Retirement [<feed>]` counts once a few
      real releases have run. First real run (2026-10-06, against the 2026-09-27 load):

      | Feed | Active before | Retired | Notes |
      |---|---|---|---|
      | jmdict | 218,868 | 1 (`1905600` 深い霧) | ~37 new entries added; ES `vocab` = 218,867 = active rows |
      | jmnedict | 743,675 | 1 (`5540981` 天文科学館) | |
      | kanji.kanjidic2 | 13,108 | 0 | |
      | kanji.radkfile, kanji.kanjivg, tatoeba | | | unchanged upstream (hash-skip) |

      About 0.0005% per feed for 9 days of churn, far below the 2% the default guard allows.
- [ ] Not done: radicals that RADKFILE drops stay in `radical` (only kanji links are cleared).
      Purging long-retired, unreferenced rows is a possible later cleanup job.

## 4. Run time of changed-file runs

V3 indexes the foreign-key columns. Without the indexes, updating existing JMdict entries
full-scanned the child tables for every entry (~25 entries/s, ~2.5 h). With them, a changed
JMdict file takes ~10 min and JMnedict ~4 min (2026-10-06). The rest of the time is
statement round trips: Hibernate deletes and inserts child rows one at a time, and `IDENTITY`
ids block insert batching. If run time matters, switch to `SEQUENCE` ids / bulk upserts.

## 5. Jreibun: not ingested

Out of scope: it isn't openly licensed or publicly downloadable. See
`docs/adr/0002-open-data-sources-only-no-jreibun.md`.

## 6. Profiles and local Docker stack: done (2026-10-23)

Spring profiles `local` (IDE), `docker` (compose service) and `prod` (AWS). There is no
default profile, so a start without one fails fast. The root `docker-compose.yml` runs
Postgres, Elasticsearch, `db-init` (creates the read-only `api_reader` login) and the
updater, which runs every feed on start-up. Commands: root `README.md`. Profiles,
properties, env vars and logins: `docs/configuration.md`.

Verified 2026-10-23: `docker compose up --build` reused the existing data (218,867 active
vocab entries), `api_reader` can read and gets "permission denied" on writes, the health
endpoint is UP, the admin API returns 401 without a token, and the image without a
profile fails on the missing datasource URL.

## 7. AWS deployment notes (not started)

The `prod` profile is ready; the infrastructure is not. Decisions made so far:
RDS Postgres 16 and **Elastic Cloud** (ES 9.x; AWS OpenSearch doesn't work with the 9.x
client), secrets as plain env vars (`docs/configuration.md`), Cognito for the admin API.

- [ ] **Image**: build `updaterComponent/Dockerfile` and push it to ECR. The build skips
      tests, so CI must run `./mvnw verify` (Testcontainers needs Docker) before it.
- [ ] **Runtime**: e.g. ECS Fargate. Run **exactly one** task: the cron is in-process,
      so two tasks would update every feed twice at once. More tasks would need a lock
      (e.g. ShedLock) or an EventBridge-scheduled task instead of `@Scheduled`.
- [ ] **Secrets**: inject `DB_*`, `ELASTICSEARCH_*` and `COGNITO_*` from Secrets Manager
      or SSM through the task definition's `secrets`. The app only reads env vars.
- [ ] **Cron time zone**: the container runs in UTC, so `0 0 3 * * *` means 03:00 UTC
      (locally too). Set `TZ` or change the cron if another time matters.
- [ ] **Memory**: the JVM uses 75% of the container memory (`-XX:MaxRAMPercentage=75`).
      Size the task from a measured JMdict run.
- [ ] **Network**: outbound HTTPS to the upstreams (EDRDG, GitHub releases for KanjiVG,
      Tatoeba) through a NAT gateway; inbound only from admin callers (internal
      ALB/VPN). Health check: `GET /actuator/health`.
- [ ] **RDS**: the datasource user owns the schema (Flyway runs as it on start-up).
      `db-init` is local only. Create the API backend's read-only role in RDS through
      infra (same grants as `docker/postgres/api_reader.sql`, with a real password).
- [ ] **Elastic Cloud**: one API key for the updater (write `vocab`, `example_sentence`)
      and a separate read-only key for the API backend.
- [ ] Observability goes to its **own** Elasticsearch/Kibana, never the search cluster
      (see 8).

## 8. Open items from the 2026-10-23 session

- [ ] **ELK monitoring, tracing and logging**: a separate design session. Decided: a
      separate Elasticsearch + Kibana for observability, never the search ES. Open:
      tracing (OpenTelemetry vs Elastic APM agent), log shipping (Filebeat, Logstash or
      an OTel collector), log format (structured JSON), and which metrics/dashboards.
      Actuator is in place (only `health` exposed).
- [ ] **Config server**: may replace the duplicated `local`/`docker` property files.
- [ ] **Container diagram** (`written_reports/Architectures/Container_Diagram_V03.JPG`):
      says MySQL; the relational DB is Postgres. Add the local Docker stack and the
      separate observability ES.
- [ ] **Overlapping runs**: admin-API and start-up runs share one queue, but the cron
      run uses the scheduler thread and can overlap a manual run of the same feed.
- [ ] **Volume names**: the root compose pins `updatercomponent_postgres-data` /
      `updatercomponent_elasticsearch-data` to keep the loaded data, so Compose warns
      "volume already exists but was created for project updatercomponent" on each `up`
      (harmless). Renaming them later means a reload or a volume copy.
- [ ] **Local Elasticsearch has no auth**: the API backend can write to it locally. A
      read-only ES user needs `xpack.security.enabled=true` in compose.
- [ ] **Cleanup**: remove `hibernate.dialect` (Hibernate warns HHH90000025: it's
      detected automatically); Spring Boot 4.1.2 is out (on 4.1.1).
