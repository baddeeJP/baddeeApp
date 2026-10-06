# updaterComponent — Next Steps

All four spokes (JMdict, JMnedict, Kanji, Tatoeba) are implemented, covered by unit and
Testcontainers integration tests, and verified end-to-end on real upstream data (2026-09-27).
Full history of that work, build/run commands and hard-won gotchas:
`../written_reports/Archived/Claude/NEXT_STEPS.md`.

## 1. Go live on AWS Cognito

The code is ready (`hub/CognitoJwtDecoderConfig`, `hub/SecurityConfig`); it needs a real user pool.

- [ ] Create (or pick) the user pool, an app client for admin callers, and a Cognito group
      `updater-admin` (or change `updater.admin.required-group`). Add admin users to the group.
- [ ] Configure the deployed environment:
  ```properties
  updater.admin.stub-auth=false
  spring.security.oauth2.resourceserver.jwt.issuer-uri=https://cognito-idp.<region>.amazonaws.com/<userPoolId>
  updater.admin.cognito.client-ids=<appClientId>
  ```
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
  `docker compose down -v`.

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

## 4. Jreibun: not ingested

Out of scope: it isn't openly licensed or publicly downloadable. See
`docs/adr/0002-open-data-sources-only-no-jreibun.md`.
