# Configuration

The updater is configured with Spring profiles. There is **no default profile**:
`application.properties` holds only settings shared by every profile, and has no
datasource URL or auth settings. Starting without a profile fails fast
("Failed to configure a DataSource").

| Profile | Where the updater runs | Databases | Admin auth | Runs feeds on start-up |
|---|---|---|---|---|
| `local` | On the host: IDE or `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | compose DBs on `localhost:5432` / `localhost:9200` | stub: `Bearer dev-token` | no |
| `docker` | As the `updater` service of the root `docker-compose.yml` | compose service names `postgres` / `elasticsearch` | stub: `Bearer dev-token` | yes |
| `prod` | AWS (not deployed yet) | env vars (RDS + Elastic Cloud) | Cognito | no |

`local` and `docker` are standalone, so their dev settings are duplicated. Change
both files together. A config server may replace this later (see NEXT_STEPS.md).

Integration tests activate `local` (`support/IntegrationTest`); Testcontainers'
`@ServiceConnection` replaces its localhost URLs.

## Files

- `application.properties`: shared settings. JPA/Flyway (`ddl-auto=validate`), cron
  `0 0 3 * * *`, `updater.run-on-startup=false`, `updater.admin.stub-auth=false`,
  required group `updater-admin`, and only the `health` Actuator endpoint exposed.
- `application-local.properties` / `application-docker.properties`: user and password
  `updater`/`updater`, `baseline-on-migrate=true` (adopts pre-Flyway databases as V1),
  stub auth with `dev-token`.
- `application-prod.properties`: every connection setting and secret comes from an env
  var with no default (see below). `baseline-on-migrate=false`.

## Updater properties

| Property | Default | Meaning |
|---|---|---|
| `updater.schedule.cron` | `0 0 3 * * *` | When every feed runs. `-` disables it (tests). |
| `updater.run-on-startup` | `false` (`true` in `docker`) | Queue a run of every feed when the app is ready. |
| `updater.<feedId>.retire-guard` | `0.98` | See ADR 0001. |
| `updater.<feed>.url` | built in | Upstream mirror override (see `application.properties`). |
| `updater.admin.stub-auth` | `false` | `true` accepts only `updater.admin.dev-token`. Never in prod. |
| `updater.admin.required-group` | `updater-admin` | Cognito group needed for `/admin/**`. |
| `updater.admin.cognito.client-ids` | none | Allowed Cognito app clients (when stub auth is off). |

Runs from the admin API and start-up share one background thread, so they queue
instead of overlapping. The cron run uses the scheduler thread.

## `prod` env vars (all required)

| Env var | Property |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | |
| `DB_URL` | `spring.datasource.url` (e.g. `jdbc:postgresql://<rds-endpoint>:5432/updater`) |
| `DB_USERNAME`, `DB_PASSWORD` | datasource login (the schema owner: Flyway runs as this user) |
| `ELASTICSEARCH_URIS` | Elastic Cloud endpoint (`https://...:443`) |
| `ELASTICSEARCH_API_KEY` | `spring.elasticsearch.api-key` (encoded API key) |
| `COGNITO_ISSUER_URI` | `https://cognito-idp.<region>.amazonaws.com/<userPoolId>` |
| `COGNITO_CLIENT_IDS` | comma-separated app client ids |

## Endpoints

- `GET /actuator/health`: open, UP/DOWN only. Used by the Docker healthcheck.
- `GET /admin/updates`, `POST /admin/updates[/{id}]`: Bearer token plus group (see
  `hub/SecurityConfig`).

## Local logins (root `docker-compose.yml`)

| What | Where | Login |
|---|---|---|
| Postgres, read-only (for other apps) | `localhost:5432/updater` | `api_reader` / `api_reader` |
| Postgres, owner (the updater) | `localhost:5432/updater` | `updater` / `updater` |
| Elasticsearch | `http://localhost:9200` | none (security off) |
| Updater admin API | `http://localhost:8081` | `Authorization: Bearer dev-token` |

`api_reader` is created by the `db-init` service (`docker/postgres/api_reader.sql`). It
has `SELECT` on every table, including tables Flyway adds later (default privileges).
