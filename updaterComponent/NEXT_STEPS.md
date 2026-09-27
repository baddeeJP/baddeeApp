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

## 2. Schema migrations

The schema is currently created by `spring.jpa.hibernate.ddl-auto=update`, which adds tables and
columns but never changes existing column types or drops anything, so type fixes silently don't
reach existing databases.

- [ ] Add Flyway (`spring-boot-starter-flyway`), generate a `V1__baseline.sql` from the current
      entity schema (Postgres 16), and switch to `ddl-auto=validate`.
- [ ] Keep the column choices the real-data run proved necessary: `text` / `text[]` for free-text
      and list columns (guarded by `UpdaterComponentApplicationIT.freeTextColumnsAreNotLengthLimited`).
- [ ] Any database created before 2026-09-27 must be reset (`docker compose down -v`): its
      column types and Tatoeba ids (now keyed on the Japanese sentence id) are wrong.

## 3. Entries removed upstream

Spokes only upsert. An entry deleted from JMdict/JMnedict/Tatoeba/KanjiDic2 upstream stays in
Postgres and Elasticsearch forever, and a kanji dropped from RADKFILE keeps its old radicals.

- [ ] Decide whether stale entries matter to the consuming search app.
- [ ] If so, one approach: during a changed-file run, collect the ids seen, then delete rows (and
      ES documents) not seen, in the same run, after a successful parse only (never on failure).
      Cover it with a `*ModuleIT` case that publishes a file with one entry removed.
