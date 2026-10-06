-- Entries whose key disappears from a changed upstream file are retired, not deleted:
-- the row (and its children) stays so ids stored elsewhere keep resolving, and is
-- marked here. Readers must filter "retired_at IS NULL".
-- See docs/adr/0001-retire-in-postgres-delete-from-search.md.
ALTER TABLE vocab_entry      ADD COLUMN retired_at timestamp(6) with time zone;
ALTER TABLE name_entry       ADD COLUMN retired_at timestamp(6) with time zone;
ALTER TABLE example_sentence ADD COLUMN retired_at timestamp(6) with time zone;
ALTER TABLE kanji            ADD COLUMN retired_at timestamp(6) with time zone;
