-- Read-only login for apps that query the dictionary data (the future API backend).
-- Run by the `db-init` compose service on every `docker compose up`, so it must
-- stay idempotent. It works on a fresh volume (before Flyway has created any
-- table) and on an existing one.

DO $$
BEGIN
	IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'api_reader') THEN
		CREATE ROLE api_reader LOGIN PASSWORD 'api_reader';
	END IF;
END
$$;

GRANT CONNECT ON DATABASE updater TO api_reader;
GRANT USAGE ON SCHEMA public TO api_reader;
-- Tables that exist now...
GRANT SELECT ON ALL TABLES IN SCHEMA public TO api_reader;
-- ...and tables Flyway (running as `updater`) creates later.
ALTER DEFAULT PRIVILEGES FOR ROLE updater IN SCHEMA public GRANT SELECT ON TABLES TO api_reader;
