# Validation results

Produced by `java -cp target/classes dev.mfgdiff.EvalMain docs/validation/results.md`. The formulas, the setup and the rule that chooses the default are in [metrics.md](metrics.md). No target value was set before measuring; the numbers below are what the program printed.

Setup: base revision = `SampleData.base(seed, 30)`; changes = `ChangeGenerator.generate(base, percent/100, seed*1000+percent)`; both revisions are written as B2MML, checked against the official schema, parsed again, and only then compared. Seeds 1 to 20 are the **tuning seeds** (part 1). Seeds 21 to 40 are the **held-out seeds**: they were not used to choose anything (part 2).

## Part 1. Grid on the tuning seeds 1 to 20

Score of a setting = segment-level errors (false positives + false negatives over changed, added, removed and moved), summed over the three shares. Lowest score wins; ties go to the fast mode, then to the lower F (metrics.md section 7).

| ratio mode | F | errors at 5% | errors at 10% | errors at 20% | score |
|---|---:|---:|---:|---:|---:|
| fast | 0.5 | 4 | 12 | 43 | 59 |
| fast | 0.6 | 4 | 12 | 43 | 59 |
| fast | 0.7 | 0 | 0 | 0 | 0 |
| fast | 0.8 | 0 | 0 | 0 | 0 |
| fast | 0.9 | 0 | 0 | 12 | 12 |
| accurate | 0.5 | 4 | 12 | 44 | 60 |
| accurate | 0.6 | 4 | 12 | 44 | 60 |
| accurate | 0.7 | 0 | 0 | 0 | 0 |
| accurate | 0.8 | 0 | 0 | 0 | 0 |
| accurate | 0.9 | 0 | 0 | 12 | 12 |

Chosen by the rule: **fast, F = 0.7** (score 0).
The default in `NodeMatcher` is fast, F = 0.7: it equals the chosen setting.

Segment-level precision / recall on the tuning seeds, per setting and share:

| ratio mode | F | share | changed P / R | added P / R | removed P / R | moved P / R |
|---|---:|---:|---|---|---|---|
| fast | 0.5 | 5% | 90.9% / 100.0% | 100.0% / 80.0% | 100.0% / 80.0% | 83.3% / 100.0% |
| fast | 0.5 | 10% | 88.9% / 100.0% | 100.0% / 80.0% | 100.0% / 75.0% | 76.9% / 100.0% |
| fast | 0.5 | 20% | 84.1% / 100.0% | 100.0% / 42.1% | 100.0% / 64.5% | 66.7% / 100.0% |
| fast | 0.6 | 5% | 90.9% / 100.0% | 100.0% / 80.0% | 100.0% / 80.0% | 83.3% / 100.0% |
| fast | 0.6 | 10% | 88.9% / 100.0% | 100.0% / 80.0% | 100.0% / 75.0% | 76.9% / 100.0% |
| fast | 0.6 | 20% | 84.1% / 100.0% | 100.0% / 42.1% | 100.0% / 64.5% | 66.7% / 100.0% |
| fast | 0.7 | 5% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.7 | 10% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.7 | 20% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.8 | 5% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.8 | 10% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.8 | 20% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.9 | 5% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.9 | 10% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| fast | 0.9 | 20% | 100.0% / 93.1% | 82.6% / 100.0% | 88.6% / 100.0% | 100.0% / 100.0% |
| accurate | 0.5 | 5% | 90.9% / 100.0% | 100.0% / 80.0% | 100.0% / 80.0% | 83.3% / 100.0% |
| accurate | 0.5 | 10% | 88.9% / 100.0% | 100.0% / 80.0% | 100.0% / 75.0% | 76.9% / 100.0% |
| accurate | 0.5 | 20% | 84.1% / 100.0% | 100.0% / 42.1% | 100.0% / 64.5% | 64.5% / 100.0% |
| accurate | 0.6 | 5% | 90.9% / 100.0% | 100.0% / 80.0% | 100.0% / 80.0% | 83.3% / 100.0% |
| accurate | 0.6 | 10% | 88.9% / 100.0% | 100.0% / 80.0% | 100.0% / 75.0% | 76.9% / 100.0% |
| accurate | 0.6 | 20% | 84.1% / 100.0% | 100.0% / 42.1% | 100.0% / 64.5% | 64.5% / 100.0% |
| accurate | 0.7 | 5% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.7 | 10% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.7 | 20% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.8 | 5% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.8 | 10% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.8 | 20% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.9 | 5% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.9 | 10% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% | 100.0% / 100.0% |
| accurate | 0.9 | 20% | 100.0% / 93.1% | 82.6% / 100.0% | 88.6% / 100.0% | 100.0% / 100.0% |

## Part 2. Held-out seeds 21 to 40: fast, F = 0.7

### Per category (sum over 20 seeds)

| changed share | level | category | applied (truth) | reported | true positives | precision | recall |
|---|---|---|---:|---:|---:|---:|---:|
| 5% | segment | same | 582 | 582 | 582 | 100.0% | 100.0% |
| 5% | segment | changed | 16 | 16 | 16 | 100.0% | 100.0% |
| 5% | segment | added | 7 | 7 | 7 | 100.0% | 100.0% |
| 5% | segment | removed | 2 | 2 | 2 | 100.0% | 100.0% |
| 5% | segment | moved | 4 | 4 | 4 | 100.0% | 100.0% |
| 5% | item | same | 1877 | 1877 | 1877 | 100.0% | 100.0% |
| 5% | item | changed | 33 | 33 | 33 | 100.0% | 100.0% |
| 5% | item | added | 43 | 43 | 43 | 100.0% | 100.0% |
| 5% | item | removed | 44 | 44 | 44 | 100.0% | 100.0% |
| 5% | item | moved | 0 | 0 | 0 | n/a | n/a |
| 10% | segment | same | 560 | 560 | 560 | 100.0% | 100.0% |
| 10% | segment | changed | 22 | 23 | 22 | 95.7% | 100.0% |
| 10% | segment | added | 11 | 10 | 10 | 100.0% | 90.9% |
| 10% | segment | removed | 18 | 17 | 17 | 100.0% | 94.4% |
| 10% | segment | moved | 17 | 18 | 17 | 94.4% | 100.0% |
| 10% | item | same | 1785 | 1785 | 1785 | 100.0% | 100.0% |
| 10% | item | changed | 61 | 62 | 61 | 98.4% | 100.0% |
| 10% | item | added | 90 | 89 | 89 | 100.0% | 98.9% |
| 10% | item | removed | 108 | 107 | 107 | 100.0% | 99.1% |
| 10% | item | moved | 0 | 0 | 0 | n/a | n/a |
| 20% | segment | same | 526 | 526 | 526 | 100.0% | 100.0% |
| 20% | segment | changed | 42 | 42 | 42 | 100.0% | 100.0% |
| 20% | segment | added | 31 | 31 | 31 | 100.0% | 100.0% |
| 20% | segment | removed | 32 | 32 | 32 | 100.0% | 100.0% |
| 20% | segment | moved | 23 | 23 | 23 | 100.0% | 100.0% |
| 20% | item | same | 1596 | 1596 | 1596 | 100.0% | 100.0% |
| 20% | item | changed | 128 | 128 | 127 | 99.2% | 99.2% |
| 20% | item | added | 174 | 174 | 174 | 100.0% | 100.0% |
| 20% | item | removed | 230 | 230 | 229 | 99.6% | 99.6% |
| 20% | item | moved | 0 | 0 | 0 | n/a | n/a |

precision = true positives / reported; recall = true positives / applied (truth). `n/a` = the denominator is 0.

### Exact runs

- 5%: 20 of 20 runs reproduced the applied changes exactly in every category and level (127 operations applied in total).
- 10%: 19 of 20 runs reproduced the applied changes exactly in every category and level (257 operations applied in total).
- 20%: 19 of 20 runs reproduced the applied changes exactly in every category and level (513 operations applied in total).

For reference, segment-level errors (false positives + false negatives, changed/added/removed/moved) over all held-out runs: 4 with fast, F = 0.7, 88 with fast, F = 0.5 (xmldiff's own default setting). This comparison was not used to choose anything.

### Kinds of difference (number of rows, all held-out runs)

| kind | 5% | 10% | 20% |
|---|---:|---:|---:|
| ITEM ADDED applied but not reported | 0 | 1 | 0 |
| ITEM CHANGED applied but not reported | 0 | 0 | 1 |
| ITEM CHANGED reported but not applied | 0 | 1 | 1 |
| ITEM REMOVED applied but not reported | 0 | 1 | 1 |
| ITEM REMOVED reported but not applied | 0 | 0 | 1 |
| SEGMENT ADDED applied but not reported | 0 | 1 | 0 |
| SEGMENT CHANGED reported but not applied | 0 | 1 | 0 |
| SEGMENT MOVED reported but not applied | 0 | 1 | 0 |
| SEGMENT REMOVED applied but not reported | 0 | 1 | 0 |

### Runs that did not match (the first 3 in order of share and seed)

#### 10% changed, seed 37

Differences between what the tool reported and what was applied (first 7 of 7):

- SEGMENT CHANGED reported but not applied: segment OP-110 "Label guard D"  <->  segment OP-901 "Assemble guard P"
- SEGMENT ADDED applied but not reported: segment OP-901 "Assemble guard P" (B side)
- SEGMENT REMOVED applied but not reported: segment OP-110 "Label guard D" (A side)
- SEGMENT MOVED reported but not applied: segment OP-110 "Label guard D"  <->  segment OP-901 "Assemble guard P"
- ITEM CHANGED reported but not applied: item P-2070 (OP-110-L2) in OP-110  <->  item P-2070 (LN-00002) in OP-901
- ITEM ADDED applied but not reported: item P-2070 (LN-00002) in OP-901 (B side)
- ITEM REMOVED applied but not reported: item P-2070 (OP-110-L2) in OP-110 (A side)

Operations applied in this run:

- ITEM_UPDATE: update item P-2002 in OP-060
- ITEM_REMOVE: remove item P-2040 from OP-030
- SEG_REMOVE: remove segment OP-110 with 2 items
- ITEM_UPDATE: update item P-2026 in OP-040
- SEG_INSERT: insert segment OP-901 after OP-280
- ITEM_INSERT: insert item P-2069 into OP-150
- SEG_RENUMBER: renumber segment OP-230 to OP-902
- SEG_RENUMBER: renumber segment OP-070 to OP-903
- ITEM_UPDATE: update item P-2053 in OP-902
- SEG_INSERT: insert segment OP-904 after OP-180
- ITEM_UPDATE: update item P-2003 in OP-210
- ITEM_UPDATE: update item P-2055 in OP-903
- ITEM_UPDATE: update item P-2070 in OP-130
- ITEM_UPDATE: update item P-2021 in OP-260

#### 20% changed, seed 23

Differences between what the tool reported and what was applied (first 4 of 4):

- ITEM CHANGED reported but not applied: item P-2018 (OP-180-L2) in OP-180  <->  item P-2018 (OP-180-L3) in OP-180
- ITEM CHANGED applied but not reported: item P-2018 (OP-180-L3) in OP-180  <->  item P-2018 (OP-180-L3) in OP-180
- ITEM REMOVED reported but not applied: item P-2018 (OP-180-L3) in OP-180 (A side)
- ITEM REMOVED applied but not reported: item P-2018 (OP-180-L2) in OP-180 (A side)

Operations applied in this run:

- ITEM_REMOVE: remove item P-2080 from OP-070
- ITEM_REMOVE: remove item P-2057 from OP-250
- ITEM_INSERT: insert item P-2033 into OP-110
- SEG_UPDATE: update segment OP-020
- ITEM_INSERT_DUP: insert duplicate row of P-2060 into OP-120
- ITEM_UPDATE: update item P-2018 in OP-180
- ITEM_UPDATE: update item P-2045 in OP-210
- ITEM_INSERT: insert item P-2026 into OP-220
- ITEM_UPDATE: update item P-2065 in OP-290
- SEG_INSERT: insert segment OP-901 after OP-130
- ITEM_INSERT: insert item P-2067 into OP-070
- ITEM_UPDATE: update item P-2003 in OP-120
- ITEM_INSERT: insert item P-2042 into OP-080
- ITEM_UPDATE: update item P-2062 in OP-060
- ITEM_INSERT: insert item P-2024 into OP-150
- ITEM_UPDATE: update item P-2066 in OP-250
- ITEM_REMOVE: remove item P-2065 from OP-050
- ITEM_INSERT: insert item P-2077 into OP-170
- ITEM_UPDATE: update item P-2041 in OP-180
- ITEM_REMOVE: remove item P-2057 from OP-100
- ITEM_UPDATE: update item P-2058 in OP-240
- SEG_REMOVE: remove segment OP-290 with 5 items
- ITEM_INSERT: insert item P-2002 into OP-080
- ITEM_REMOVE: remove item P-2018 from OP-180

Only 2 held-out runs did not match, so fewer than 3 are shown.
