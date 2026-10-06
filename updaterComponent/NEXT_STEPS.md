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

## 3. Entries removed upstream

Spokes only upsert. An entry deleted from JMdict/JMnedict/Tatoeba/KanjiDic2 upstream stays in
Postgres and Elasticsearch forever, and a kanji dropped from RADKFILE keeps its old radicals.

- [ ] Decide whether stale entries matter to the consuming search app.
- [ ] If so, one approach: during a changed-file run, collect the ids seen, then delete rows (and
      ES documents) not seen, in the same run, after a successful parse only (never on failure).
      Cover it with a `*ModuleIT` case that publishes a file with one entry removed.
