# Retire removed entries in Postgres, delete them from Elasticsearch

Status: accepted (2026-10-06)

When an entry's key disappears from a changed upstream file, the updater deletes its
Elasticsearch document and **retires** its Postgres row (`retired_at` is set, and the row and
its children are kept). Changed entries are updated in place under the same key, never deleted.
Search must stop showing removed entries, such as a JMdict duplicate that was merged away or a
deleted Tatoeba sentence. But the planned API backend will store entry ids in user lists, so the
rows must keep resolving. Retiring also makes a bad run reversible, and the row shows why an
entry disappeared.

## Rules

- **When:** only at the end of a fully successful changed-file run. A failed or partial run, or a
  hash-skip, retires nothing.
- **Guard:** `updater.<feedId>.retire-guard` (default `0.98`) is the smallest fraction of active
  entries a run may leave active. A file below it retires nothing and logs a WARN, while its
  upserts still apply. A truncated gzip already fails the parse, so the guard only has to catch
  valid-but-wrong files (a partial mirror, a parser bug). Real removals are expected to be far
  under 1% per release. A false block costs little, because stale entries stay searchable until
  someone checks. A loose guard costs a lot: 90% on JMdict would allow ~21,800 removals in one run.
  Every changed-file run logs the would-retire count so the guards can be tuned from data.
  A blocked file is still marked processed, so its removals are re-checked only when upstream
  changes again. Lowering the guard applies from the next changed file.
  For a Kanji sub-source, the guard counts the characters that sub-source has data on, not all
  active kanji rows.
- **Un-retire:** a retired key that comes back is upserted with `retired_at` cleared and
  re-indexed.
- **Order:** Elasticsearch deletes go before the Postgres update. If the update fails, the rows
  are still active, so the next run deletes and retires them again.
- **Kanji:** KanjiDic2, RADKFILE and KanjiVG each own different fields of one row. A sub-source
  that no longer lists a character hard-deletes only its own data (a correction, not a removal).
  The row is retired once no sub-source has data on it.
- **Scope:** a spoke only retires rows of its own source (Tatoeba: `source = 'tanaka-corpus'`).

## Consequences

- Anything that reads these tables (the future API backend) **must filter
  `retired_at IS NULL`**. Put that in one place there, as a view or a default repository filter,
  so it is hard to forget.
- Retired rows accumulate. Purging long-retired rows that nothing references is a possible later
  cleanup job, not done now.

## Considered options

- **Hard delete everywhere:** this breaks stored ids and cannot be undone after a bad file.
- **Keep everything (the previous behaviour):** removed entries stay searchable forever.
