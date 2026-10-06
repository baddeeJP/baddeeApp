-- Postgres doesn't index foreign-key columns by itself. Without these, updating an existing
-- JMdict entry (load its readings/senses, delete the replaced ones) full-scans 250k-500k-row
-- tables for every entry: the first real changed-file run managed ~25 entries/s.
-- UpdaterComponentApplicationIT.everyForeignKeyColumnIsIndexed guards new foreign keys.
CREATE INDEX reading_ent_seq_idx        ON reading (ent_seq);
CREATE INDEX sense_ent_seq_idx          ON sense (ent_seq);
CREATE INDEX sense_gloss_sense_id_idx   ON sense_gloss (sense_id);
CREATE INDEX sense_pos_sense_id_idx     ON sense_pos (sense_id);
CREATE INDEX sense_misc_sense_id_idx    ON sense_misc (sense_id);
CREATE INDEX kanji_radical_radical_idx  ON kanji_radical (radical);
