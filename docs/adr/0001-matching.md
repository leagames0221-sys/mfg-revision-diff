# ADR 0001: How a step or bill row of revision A is matched to one of revision B

Status: accepted

Source entries (S...) and author's decisions (D...) refer to [../sources.md](../sources.md).

## Context

The tool must say, for every step (OperationsSegment) and every bill row (OperationsMaterialBillItem) of revision A, which object of revision B it is, or that there is none. Everything else (same, changed, added, removed, moved) follows from that pairing.

Facts that shape the pairing:

- **The ID is not a global key.** B2MML says its element IDs are "not intended to act as global object IDs or database index attributes" (S1.7). A step can be renumbered, and two steps can carry the same ID.
- **Other tools break on exactly these cases.** ERPNext kept only the last row of a part and hid additions and removals (S4.1); Odoo could not tell lines of one product apart when operation IDs changed (S4.3); an InvenTree check value changed when only the row order changed (S4.4).
- **Bill rows have no order, steps do.** "BOMs are unordered" (S4.5); the order of a process plan is part of its meaning (Zhang and Shasha work on trees where "the left-to-right order among siblings is significant", S3.13).
- **Names alone are too loose.** On bills of material, matching on names alone gave 743 candidate matches and a structural constraint removed 880 of them (S3.12).
- **No public sample has two revisions** (sources.md, N1), so the pairing must be testable with data made by a generator (docs/validation/metrics.md).

## Decision

We will pair in three steps. The structure is that of xmldiff `diff.py` (MIT licence); the code is written from scratch in Java and the port of the longest common subsequence is credited in NOTICE.

1. **Key.** A segment whose `ID` occurs exactly once on each side is paired by that ID alone. Bill rows that share a part key (`MaterialSpecificationID`, D1) are paired among themselves: rows with identical content first, then the remaining ones in order of appearance (D7). A bill row that has a part key is never paired with a row that has another key.
2. **Similarity.** The segments that are still unpaired are paired when their similarity is at least F. The similarity is `sqrt((own^2 + child^2) / 2)`, where `own` is the ratio of the two nodes' texts and `child` is the share of bill rows that could be paired between the two segments. Perfect matches are taken first, then the best candidates in descending order, then a greedy pass, as in xmldiff's `best_match` mode. xmldiff has two ways to compute `own`: `fast` (difflib `quick_ratio`, the xmldiff default) and `accurate` (difflib `ratio`, the Ratcliff/Obershelp longest-matching-block method); both are ported and selectable (`--ratio`). **The default is `fast` with F = 0.7** (xmldiff's own is `fast`, 0.5). It was chosen by a rule written before the measurement, on tuning seeds 1 to 20, and reported on separate seeds 21 to 40 (docs/validation/metrics.md section 7, results.md, D17). xmldiff says of F: "Set the value high, and you will see more nodes inserted and deleted instead of being updated." (S3.19).
3. **Order.** The paired segments, listed in the order of A and in the order of B, are compared with a longest common subsequence. A pair that is not on it is **moved**.

Bill rows are compared as an unordered collection, so reordering rows is never a change (S4.5, S4.4).

### Mapping to the sources

| xmldiff (upstream line) | what it does | this repository |
|---|---|---|
| diff.py L20 `F = 0.5` | default threshold | `NodeMatcher.java` L23 `DEFAULT_F` (0.7 here, see Decision and D17) |
| diff.py L26, L207-L217 | a unique attribute decides alone; a different value means no match | `NodeMatcher.matchSegments` L88-L100 (segment IDs, only when unique on both sides); `NodeMatcher.matchItems` L107-L148 (part keys; a different key never pairs) |
| diff.py L34 `quick_ratio` (ratio_mode `fast`) | own-text ratio | `QuickRatio.java` L15; port of difflib `quick_ratio` (CPython `Lib/difflib.py` L622, `_calculate_ratio` L39) |
| diff.py L35-L36 `ratio_mode == "accurate"` (`ratio`) | own-text ratio, longest matching blocks | `AccurateRatio.java` L123 `ratio`, L60 `findLongestMatch`, L101 `matchedSize`, L41 `chainB`; port of difflib `ratio` L597, `find_longest_match` L305, `get_matching_blocks` L421, `__chain_b` L266 (CPython v3.12.0, with isjunk=None and autojunk=True as xmldiff creates it) |
| diff.py L226-L244 `node_text` | text of a node: tag, text, sorted attributes | `Node.text` L75 (kind label and sorted `name:value` pairs; D8) |
| diff.py L219-L224 `node_ratio` and L262-L278 `child_ratio` | `sqrt((match**2 + child_ratio**2) / 2)`, child share = matched children / larger child count | `NodeMatcher.segmentSimilarity` L151-L159 |
| diff.py L136-L169 `best_match` | perfect matches, then descending score | `NodeMatcher.pairBySimilarity` L177-L212 |
| diff.py L171-L192 | greedy pass over what is left | `NodeMatcher.pairBySimilarity` L213-L230 |
| diff.py L384-L426 `align_children`, L399 LCS, L418 `MoveNode` | children not on the LCS are moved | `Differ.diff` L30-L46 and `Lcs.compute` (`Lcs.java` L21) |
| utils.py L40 `longest_common_subsequence` | Myers O(ND) LCS | `Lcs.java` (port) |

Differences from xmldiff, with the reason:

- xmldiff works on one unordered pool of nodes of a whole document. Here the pairing is done level by level: segments first (their similarity asks the bill-row pairing of each candidate pair), bill rows inside a pair of segments. The reason is that a part number repeats across steps, so it is only a key inside one bill.
- xmldiff gives a unique attribute that differs the score 0. For segments this is relaxed, because a renumbered step must still be found (D6). For bill rows it is kept.
- xmldiff's text includes attribute names as they are; B2MML fields are child elements, so they are flattened into `name:value` pairs (D8).
- Same-key rows use two passes (D7). The ERPNext expectation that reordering "should not produce false changes" (S4.1) is why identical rows are taken first.

### Why "moved" is a category of its own

SAP's routing comparison has the symbols `=`, not-equal, `A` and `B` (S4.6). Oracle's filter has All, Differences, Added, Changes, Removed, Unchanged and compares the item sequence as one attribute among others (S4.7). ERPNext's expectation is that reordering is not a change at all (S4.1), and InvenTree sorts its result by name (S4.2), so a reorder cannot be seen. In the pages that were readable, no product reports a step that has the same content and a new position under its own name (sources.md, N4). For a process plan the sequence matters, so this tool reports it as **moved**, next to same or changed, because a step can be both moved and changed (D10).

## Alternatives considered

| candidate | what it does | why not |
|---|---|---|
| **ID only** | pair by `ID` | B2MML IDs are not global (S1.7); a renumbered step becomes one removal plus one addition; ERPNext keyed by item code and lost duplicates (S4.1); Odoo's duplicated operation IDs gave false changes (S4.3). Kept as step 1, but only when unique on both sides. |
| **Position only** | row i of A against row i of B | One inserted step shifts every later step; a library that does this says so: "This will compare in sequence order." (S3.10). A tool that offers a sequence number as an index depends on the numbers being stable (S3.11). |
| **Name similarity only** | pair by fuzzy text | On bills of material it produced 743 candidates (S3.12). Kept as step 2, but only after keys, and combined with the share of matched children. |
| **Zhang-Shasha tree edit distance** | cheapest insert/delete/modify script on ordered trees | The order of steps matches its model (S3.13), but it has no move: a reordered step becomes a deletion plus an insertion. Worst case O(n^2 m^2) (S3.14). |
| **GumTree two stages** | stage 1: identical subtrees top-down; stage 2: parents with many already paired descendants; moves reported | Same family as the chosen method (S3.15, S3.16) and it detects moves. Its code is LGPL (S3.17), so it cannot be copied; only the mechanism is used. The chosen method has the same two stages (keys and identical rows, then shared children) and is under a licence that allows a port with attribution. |
| **xmldiff matching** | unique key, threshold F with combined similarity, LCS for moves | **Chosen.** MIT; one document that contains the key rule, the threshold, the child share and the move rule (S3.1 to S3.3). |

X-Diff (S3.18) was also read: it treats siblings as unordered and has no move operation, so it fits the bill rows but not the steps.

## Consequences

- Positive: reordered steps are "moved", not "changed"; reordered bill rows are "same"; duplicate IDs and duplicate part rows do not produce false changes (tests in `OssFailureScenariosTest`, see sources.md section 8).
- Negative, measured: the text ratio is a count of shared characters (fast) or of matching blocks (accurate), and two unrelated steps of the same kind of text score high with either. At xmldiff's own F = 0.5 a removed step and an added step are usually paired as one **changed** step, which also marks it **moved** when the positions differ. On the held-out seeds this costs 88 segment-level errors at F = 0.5 against 4 at the chosen F = 0.7 (results.md part 2). The `accurate` mode did not do better than `fast` on the tuning seeds (same score at every F that reached 0, 60 against 59 at F = 0.5) and is slower, so the default stays `fast`.
- Negative: F = 0.7 still pairs a removed and an added step when they look alike enough; one held-out run (10% changed, seed 37) shows it. A value of F that never makes this mistake would also stop pairing steps whose text changed a lot; at F = 0.9 the tuning seeds already show missed pairings (12 errors at 20%).
- Limit of the choice: 0.7 and 0.8 both reached 0 errors on the invented tuning data, so the tie rule picked 0.7. Real step descriptions are not random strings; the value may need to be raised or lowered for them (`--f`).
- Negative: a pair of rows with the same part key, one of which was deleted and the other changed, can be paired in the wrong way (order of appearance). It is visible in `results.md`.
- Negative: when several orders have the same longest common subsequence (for example two neighbours swapped), the tool marks the one the ported algorithm leaves out. Which of the two is reported as moved is not a statement about which one a person moved.
- Cost: the similarity of each candidate pair of segments pairs their bill rows again; fine for hundreds of steps, quadratic in the number of steps.
- Not covered: nested segments, multi-level bills, dependencies (D5); they are reported as warnings.
