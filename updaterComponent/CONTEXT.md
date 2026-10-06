# Updater

Keeps a Postgres copy of the open Japanese dictionary sources up to date, plus a search copy in Elasticsearch.

## Language

**Feed**:
One upstream file the updater tracks by content hash (e.g. `jmdict`, `kanji.radkfile`).
_Avoid_: source file, dataset

**Spoke**:
The module that fetches, parses and stores one source (JMdict, JMnedict, Kanji, Tatoeba). A spoke may own several feeds.
_Avoid_: plugin, connector

**Sub-source**:
One of the three feeds (KanjiDic2, RADKFILE, KanjiVG) that together fill a single kanji row; each owns its own fields.

**Entry**:
One top-level record keyed by its stable upstream key: a vocab entry (`ent_seq`), a name (`ent_seq`), an example sentence (Tatoeba id), or a kanji (its character).
_Avoid_: item, record, word

**Active entry**:
An entry that the latest successfully processed file of its feed still contains.

**Retired entry**:
An entry whose key disappeared from upstream. Its row is kept so stored ids still resolve, it is gone from search, and it becomes active again if the key comes back.
_Avoid_: deleted, archived, soft-deleted

**Retire guard**:
The smallest fraction of active entries that one run may leave active. A file below it retires nothing.
