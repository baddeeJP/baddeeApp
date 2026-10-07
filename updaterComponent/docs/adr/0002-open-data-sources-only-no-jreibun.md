# Only openly licensed sources; Jreibun is not ingested

Status: accepted (2026-10-06)

The updater only ingests sources with an open licence and a stable public download URL that a
scheduled job can fetch. Jreibun is listed next to Tatoeba under "example sentences" in
`written_reports/Architectures/Container_Diagram_V03.JPG`, but it is out of scope.
Version 1.0 (2026-09-10, 18,877 sentences) is distributed only through GSK: you apply by email
with a signed pledge form, pay ¥33,000 unless you are a GSK member, and receive the files by
manual file share. Use is limited to education, research and non-profit purposes. JMdict's own
examples file (`JMdict_e_examp.gz`, checked 2026-10-06) contains only Tatoeba sentences.

Sources: the Jreibun project page
(https://www.tufs.ac.jp/ts/personal/SUZUKI_Tomomi/jreibun/index-jreibun.html) and the GSK
catalogue entry (https://www.gsk.or.jp/catalog/gsk2026-a/).

## Consequences

`example_sentence` stays keyed by the Tatoeba Japanese sentence id. The `(source, id)` re-key
that a second sentence source would need is not done. Do it only when an open second source
is added.
