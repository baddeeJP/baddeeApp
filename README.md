# baddeeApp
This project aims to support Japanese language learners, and those who are trying to navigate Japanese media or Japanese society in the trials and triumphs of the Japanese language.

## Local stack

Needs Docker. From the repo root:

```sh
docker compose up --build -d                          # databases + updater (fills/updates the data)
docker compose up -d postgres elasticsearch db-init   # databases only
docker compose logs -f updater                        # watch a run
docker compose down                                   # stop (data is kept in volumes)
```

| What | Where | Login |
|---|---|---|
| Postgres (for other apps) | `localhost:5432`, database `updater` | `api_reader` / `api_reader` (read-only) |
| Elasticsearch | `http://localhost:9200` | none |
| Updater admin API | `http://localhost:8081` | `Authorization: Bearer dev-token` |

On the first `up` with empty volumes, the updater loads every feed. That takes tens of
minutes; follow it with `docker compose logs -f updater`. Later starts re-check every
feed and skip the unchanged ones.

To run the updater from the IDE instead, start the databases only and use the Spring
profile `local`. Profiles and settings: `updaterComponent/docs/configuration.md`.

**Moving from the old `updaterComponent/docker-compose.yml`:** the new stack reuses its
data volumes. Remove the old stopped containers once:
`docker rm updater-postgres updater-elasticsearch`.
