# How correctness is measured

Source entries (S...) and author's decisions (D...) refer to [../sources.md](../sources.md). No target value was set for any number in this directory; the numbers in [results.md](results.md) are what the program printed.

## 1. The idea

A change generator takes a first revision (A) and makes a second revision (B) by applying random operations. It also writes down what it did (the truth). The tool then reads A and B as files, as a user would, and its answer is compared with the truth.

The generator follows the change generator of the X-Diff paper, section 5: it applies insert, delete and update "randomly at each level", and "takes a parameter, the percentage of nodes being changed" (S5.1). That paper's generator "does not permute the order of any nodes", because the compared algorithm there did not permute; this tool must report moved steps, so the generator also has a **move** operation. The names insert, remove, update, move come from the description of tree-difference tools in S5.2. **Renumber** (the ID changes and nothing else) is added by the author (D12).

## 2. Operations

`ChangeGenerator` picks nodes (steps and bill rows) at random, each at most once, until `max(1, round(share x (steps + rows)))` operations have been applied. The operation is chosen at random for the kind of node:

| operation | on | effect |
|---|---|---|
| `SEG_INSERT` | step | a new step (0 to 3 rows) after the chosen step |
| `SEG_REMOVE` | step | the step and its rows are deleted |
| `SEG_UPDATE` | step | one of description, duration, station gets another value |
| `SEG_MOVE` | step | the step goes to another position, at least 2 places away |
| `SEG_RENUMBER` | step | the ID gets a new value that was not used; everything else stays |
| `ITEM_INSERT` | row | a new row with a part not yet in the bill |
| `ITEM_INSERT_DUP` | row | a new row for a part that already has a row, with another quantity (one in four row inserts) |
| `ITEM_REMOVE` | row | the row is deleted |
| `ITEM_UPDATE` | row | quantity or description gets another value |

After the operations the positions (`Indice`) of B are renumbered 10, 20, 30, ... so the document order and the position numbers agree (D12). The minimum move distance of 2 exists because moving a step by one place gives the same result as moving its neighbour; with distance 2 or more, the set of moved steps is unique when there is a single move.

`GeneratorTest` applies **each operation kind alone, once**, to twelve different base revisions and requires that the tool's answer equals the truth exactly in every category and level.

## 3. What the truth says

Each object has an identity number that the generator gave it (not stored in the files). The truth is derived from the two models:

- **same**: present in A and B, all compared fields equal;
- **changed**: present in both, at least one compared field differs;
- **added**: present only in B; **removed**: present only in A. Rows of an added (removed) step are added (removed) with it;
- **moved**: steps the generator moved. A moved step can be same or changed as well. Rows have no moved category (they are unordered).

The tool's rows are turned into the same kind of key: `A|B` for same, changed and moved, `|B` for added, `A|` for removed. A pair that joins two different objects is therefore a miss in two places: the pair it should have been (false negative) and the wrong pair (false positive).

## 4. Formulas

For each level (step, row) and category, over a set of runs:

- tp = reported rows that are in the truth, fp = reported rows that are not, fn = truth rows that were not reported;
- **precision = tp / (tp + fp)** and **recall = tp / (tp + fn)** (S5.3);
- the runs of one share are added up first (all 20 seeds, tp, fp and fn summed) and then divided; `n/a` means the denominator is 0 (for example no row is ever moved);
- an **exact run** is one where fp and fn are 0 in every category and level.

## 5. Setup of the tables in results.md

| item | value |
|---|---|
| shares of changed nodes | 5 %, 10 %, 20 % |
| tuning seeds | 1 to 20 for each share: the grid of ratio mode x F (part 1 of results.md) |
| held-out seeds | 21 to 40 for each share: only the chosen default (part 2 of results.md) |
| base revision | `SampleData.base(seed, 30)`: 30 steps, 0 to 6 rows each (about 105 rows), 6 % of rows followed by a duplicate row of the same part (D13) |
| operation seed | `seed x 1000 + percent` |
| matcher | `NodeMatcher` with the ratio mode and F of the setting under test; default fast, F = 0.7 (D17) |
| path | A and B are written as B2MML, **checked against the official schema**, read again, then compared |

Reproduce: `mvn -o test` (builds), then `java -cp target/classes dev.mfgdiff.EvalMain docs/validation/results.md`. The program is deterministic.

## 6. Where the truth itself is ambiguous

These cases make the tool "wrong" against the truth although the answer is a valid reading of the two files:

- **Two rows of the same part, one deleted and the other changed.** The tool pairs same-part rows by content and then in order of appearance (D7). If the earlier row was the one deleted, the tool reports the earlier row as changed and the later one as removed.
- **Neighbours swapped, or several moves at once.** When more than one order has the longest common subsequence, the tool reports the steps the algorithm leaves out, not necessarily the ones the generator moved.
- **A removed step and an added step that look alike.** The tool's similarity cannot tell them from one changed step (ADR 0001, Consequences). This is a weakness of the method, not of the truth, and it is the main source of the differences in results.md.

## 7. How the default ratio mode and F are chosen (rule written before the grid was run)

A measurement at xmldiff's own default (F = 0.5, fast ratio) on seeds 1 to 20 showed that removed and added steps are often paired as one changed step. xmldiff names two settings that act on this (S3.19): `ratio_mode` ("accurate" or "fast") and `F` ("Set the value high, and you will see more nodes inserted and deleted instead of being updated"). The settings must not be chosen and reported on the same data: testing on the same data as the one used for learning the parameters "is a methodological mistake" (S5.5; the sentence reads "same data is a methodological mistake" in the saved page). So:

1. **Tuning seeds: 1 to 20. Reporting seeds: 21 to 40.** The seeds 21 to 40 are not used for any choice.
2. **Grid:** ratio mode {fast, accurate} x F {0.5, 0.6, 0.7, 0.8, 0.9} x share {5 %, 10 %, 20 %}, 20 seeds per cell (`EvalMain` writes the grid table).
3. **Score of a setting (mode, F):** the number of segment-level errors, that is fp + fn summed over the four segment categories changed, added, removed and moved, summed over the three shares (60 runs). The setting with the **lowest score** is the default.
4. **Ties:** the fast mode wins over the accurate mode (it is cheaper); then the lower F wins (closer to the xmldiff default 0.5).
5. After the choice, the chosen setting is run once on seeds 21 to 40 and the full per-category table is reported. No setting is changed after seeing that table. If it is below 100 % no target is set; the misses are shown.

The rule above was written before the grid was run. The outcome is written in D17 of sources.md and in part 1 of results.md. Because several settings reach 0 errors on the tuning seeds, the tie rule decided between them; this is a limit of the invented data, not evidence that 0.7 is better than 0.8.
