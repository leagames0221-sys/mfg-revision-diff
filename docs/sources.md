# Sources

Every rule or value in this repository that has a source is listed here with the URL, the saved copy of the page or file, and a verbatim quote.

How to read this file:

- A line that starts with `> ` is a verbatim quote. It is a substring of one line of the saved copy named in the same entry (`Saved copy`). The saved copies are text files kept outside this repository. Nothing else in this file is a quote.
- The saved copies carry two header lines (`URL: ...` and a retrieval date). For source code files the line numbers below are the upstream line numbers (saved line minus 2); for other files they are saved line numbers.
- Everything that has no source is listed under "Author's decisions". Places where a source was required but none was found are listed under "Searched, not found".


## 1. Data shape: B2MML (docs/adr/0002-data-shape-b2mml.md)

### S1.1 B2MML is the XML form of ISA-95

- Used for: choice of data shape
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/README.md
- Saved copy: `b2mml_README.txt` (fetched 2026-10-29)
- Quote (saved line 4):
> B2MML is an XML implementation of the ANSI/ISA-95

### S1.2 Schema version and namespace

- Used for: bundled schema version; namespace check in SecureXml and the reader
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Schema/B2MML-ProcessSegment.xsd
- Saved copy: `b2mml_xsd_ProcessSegment.txt` (fetched 2026-10-29)
- Quote (saved line 24):
> Copyright 2023 MESA International, Version 0701
- Quote (saved line 6):
> http://www.mesa.org/xml/B2MML

### S1.3 Segment, bill and bill item element names

- Used for: element names written and read (OperationsDefinition, OperationsSegment, OperationsMaterialBill, OperationsMaterialBillItem)
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Schema/B2MML-OperationsDefinition.xsd
- Saved copy: `b2mml_xsd_OperationsDefinition.txt` (fetched 2026-10-29)
- Quote (saved line 114):
> <xsd:complexType name = "OperationsDefinitionType">
- Quote (saved line 206):
> <xsd:complexType name = "OperationsSegmentType">
- Quote (saved line 150):
> <xsd:complexType name = "OperationsMaterialBillType">
- Quote (saved line 175):
> <xsd:complexType name = "OperationsMaterialBillItemType">

### S1.4 How a segment points to its bill and a bill item to its material

- Used for: OperationsMaterialBillID links a segment to a bill; MaterialSpecificationID is the part key (Author's decision D1)
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Schema/B2MML-OperationsDefinition.xsd
- Saved copy: `b2mml_xsd_OperationsDefinition.txt` (fetched 2026-10-29)
- Quote (saved line 141):
> <xsd:element name = "OperationsMaterialBillID"
- Quote (saved line 198):
> <xsd:element name = "MaterialSpecificationID"
- Quote (saved line 239):
> <xsd:element name = "ParameterSpecification"

### S1.5 Where the position number goes: ParameterSpecification (ParameterType = ID + Value)

- Used for: the 'Indice' position number of a segment is written as a ParameterSpecification
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Schema/B2MML-Common.xsd
- Saved copy: `b2mml_xsd_Common.txt` (fetched 2026-10-29)
- Quote (saved line 1384):
> <xsd:complexType name="ParameterType">

### S1.6 The bundled example carries the position number as a property with ID 'Indice'

- Used for: name and value shape of the position number
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Examples/Courbon%20B2MML%20v0401%20Example%20XML%20Files.zip
- Saved copy: `ex_Courbon_PRO-20121210181416-27942.xml.txt` (fetched 2026-10-29)
- Quote (saved line 103):
> <ID>Indice</ID>
- Quote (saved line 107):
> <ValueString>0010</ValueString>
- Quote (saved line 101):
> <MaterialRequirementProperty>

### S1.7 Element IDs are not global IDs

- Used for: why the ID alone is not the matching rule (ADR 0001); segment IDs may be duplicated or renumbered
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Documentation/B2MML-Documentation.pdf
- Saved copy: `b2mml_Documentation.txt` (fetched 2026-10-29)
- Quote (saved line 190):
> The element IDs are defined only to identify objects within related exchanged information sets.
- Quote (saved line 191):
> intended to act as global object IDs or database index attributes.

### S1.8 Licence: use is free, MESA must be acknowledged

- Used for: NOTICE and README acknowledgement
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/LICENSE
- Saved copy: `b2mml_LICENSE.txt` (fetched 2026-10-29)
- Quote (saved line 13):
> without fee or royalty is hereby granted provided MESA International
- Quote (saved line 17):
> The Business To Manufacturing Markup Language (B2MML) is used
- Quote (saved line 18):
> courtesy of MESA International.

### S1.9 Example files differ in version

- Used for: why the sample data is written against the current 0701 schema and validated
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/Examples/Readme.txt
- Saved copy: `b2mml_Examples_Readme.txt` (fetched 2026-10-29)
- Quote (saved line 4):
> check the version the file is based on before using it

Searched in the saved copy of the schema (`b2mml_xsd_OperationsDefinition.txt`): the string `Property` occurs 0 times and `MaterialDefinitionID` occurs 0 times. So an OperationsSegment has no property element of its own and an OperationsMaterialBillItem has no MaterialDefinitionID (Author's decisions D1 and D3).

### Alternatives for the data shape (ADR 0002)

### S1.10 STEP

- Used for: STEP is not free
- URL: https://en.wikipedia.org/wiki/ISO_10303
- Saved copy: `wiki_iso10303.txt` (fetched 2026-10-29)
- Quote (saved line 123):
> Excepting few underlying standards ISO10303 is not free
- Quote (saved line 245):
> AP 240, Process plans for machined products
- Quote (saved line 215):
> AP 239, Product life cycle support
- Quote (saved line 137):
> AP 242, Managed model based 3D engineering

### S1.11 PLCS

- Used for: the OASIS committee is closed
- URL: https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=plcs
- Saved copy: `oasis_plcs_tc.txt` (fetched 2026-10-29)
- Quote (saved line 38):
> The Technical Committee was closed by TC Administration on 17 April 2017

### S1.12 IPC-2581

- Used for: circuit-board data, not assembly-line process plans
- URL: https://www.ipc2581.com/
- Saved copy: `ipc2581_home.txt` (fetched 2026-10-29)
- Quote (saved line 31):
> also known as IPC-2581 is a generic standard for printed circuit board

### S1.13 CycloneDX

- Used for: has a manufacturing-declaration field; its reference key is only unique inside one file
- URL: https://raw.githubusercontent.com/CycloneDX/specification/master/schema/bom-1.6.schema.json
- Saved copy: `cyclonedx_bom16_schema.txt` (fetched 2026-10-29)
- Quote (saved line 105):
> Describes how a component or service was manufactured or deployed.
- Quote (saved line 126):
> Every bom-ref must be unique within the BOM.

### S1.14 ISA-95 text is paid

- Used for: the standard text behind B2MML cannot be read without paying; the XSD and its documentation are free
- URL: https://webstore.iec.ch/en/publication/6676
- Saved copy: `iec62264_2_webstore.txt` (fetched 2026-10-29)
- Quote (saved line 125):
> 475.-

### S1.15 B2MML JSON form is unchecked

- Used for: the JSON form of the schema is not used
- URL: https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/master/README.md
- Saved copy: `b2mml_README.txt` (fetched 2026-10-29)
- Quote (saved line 10):
> There has been no checking of this file.


## 2. Safe XML reading (SecureXml.java)

### S2.1 OWASP XML External Entity Prevention Cheat Sheet, Java

- Used for: DOCTYPE rejected, secure processing on, XInclude off, schema access restricted, streams not file names, stop when a setting is unsupported
- URL: https://raw.githubusercontent.com/OWASP/CheatSheetSeries/master/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.md
- Saved copy: `owasp_xxe_cheatsheet.txt` (fetched 2026-10-29)
- Quote (saved line 11):
> Reject DOCTYPE declarations if the parser supports it.
- Quote (saved line 138):
> dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
- Quote (saved line 139):
> dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
- Quote (saved line 13):
> Keep XInclude disabled unless explicitly required
- Quote (saved line 15):
> Treat an unsupported security setting as a configuration failure.
- Quote (saved line 118):
> Pass streams rather than untrusted filenames or URLs
- Quote (saved line 203):
> sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
- Quote (saved line 248):
> Reject unapproved identifiers
- Quote (saved line 17):
> Preventing XXE does not prevent every XML denial-of-service attack.


## 3. Matching (docs/adr/0001-matching.md)

### S3.1 xmldiff diff.py, default threshold, unique attribute, similarity

- Used for: NodeMatcher: F = 0.5, unique key decides alone, composite similarity of own text and matched children
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/xmldiff/diff.py
- Saved copy: `xmldiff_diff.py.txt` (fetched 2026-10-29)
- Quote (upstream line 20):
> F = 0.5
- Quote (upstream line 26):
> uniqueattrs = ["{http://www.w3.org/XML/1998/namespace}id"]
- Quote (upstream line 215):
> # One of the nodes have a unique attribute, we check only that.
- Quote (upstream line 34):
> self._sequence_ratio = self._sequencematcher.quick_ratio
- Quote (upstream line 223):
> match = sqrt((match**2 + child_ratio**2) / 2)
- Quote (upstream line 278):
> return count / child_count

### S3.2 xmldiff diff.py, perfect matches first, then by descending score, then greedy

- Used for: NodeMatcher.pairBySimilarity
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/xmldiff/diff.py
- Saved copy: `xmldiff_diff.py.txt` (fetched 2026-10-29)
- Quote (upstream line 136):
> elif self.best_match:
- Quote (upstream line 139):
> # First find all nodes that match perfectly
- Quote (upstream line 162):
> for lnode, rnode, max_match in sorted(
- Quote (upstream line 165):
> if max_match >= self.F and rnode in rnodes:

### S3.3 xmldiff diff.py, order by longest common subsequence, the rest are moved

- Used for: Differ.diff: a pair outside the LCS of the paired order is 'moved'
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/xmldiff/diff.py
- Saved copy: `xmldiff_diff.py.txt` (fetched 2026-10-29)
- Quote (upstream line 399):
> lcs = utils.longest_common_subsequence(
- Quote (upstream line 418):
> yield actions.MoveNode(

### S3.4 xmldiff API: different unique value means no match, whatever the similarity

- Used for: bill items with different part keys are never paired
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/docs/source/api.rst
- Saved copy: `xmldiff_api.rst.txt` (fetched 2026-10-29)
- Quote (saved line 126):
> the nodes will not match regardless of their structural similarity.

### S3.5 xmldiff utils.py, longest common subsequence (Myers)

- Used for: Lcs.java is a port
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/xmldiff/utils.py
- Saved copy: `xmldiff_utils.py.txt` (fetched 2026-10-29)
- Quote (upstream line 35):
> # LCS from Myers: An O(ND) Difference Algorithm and Its Variations. This
- Quote (upstream line 40):
> def longest_common_subsequence(left_sequence, right_sequence, eqfn=eq):

### S3.6 Python difflib documentation: ratio and quick_ratio

- Used for: QuickRatio.java
- URL: https://docs.python.org/3/library/difflib.html
- Saved copy: `python_difflib_docs.txt` (fetched 2026-10-29)
- Quote (saved line 474):
> number of matches, this is 2.0*M / T.
- Quote (saved line 481):
> Return an upper bound on ratio() relatively quickly.

### S3.7 Python difflib.py (CPython v3.12.0): quick_ratio

- Used for: QuickRatio.java is a port
- URL: https://raw.githubusercontent.com/python/cpython/v3.12.0/Lib/difflib.py
- Saved copy: `cpython_3.12.0_difflib.py.txt` (fetched 2026-10-29)
- Quote (upstream line 629):
> # viewing a and b as multisets, set matches to the cardinality
- Quote (upstream line 620):
> return _calculate_ratio(matches, len(self.a) + len(self.b))
- Quote (upstream line 41):
> return 2.0 * matches / length

### S3.8 xmldiff licence

- Used for: NOTICE
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/LICENSE.txt
- Saved copy: `xmldiff_LICENSE.txt` (fetched 2026-10-29)
- Quote (saved line 5):
> Permission is hereby granted, free of charge, to any person obtaining a copy

### S3.9 Python licence

- Used for: NOTICE
- URL: https://raw.githubusercontent.com/python/cpython/v3.12.0/LICENSE
- Saved copy: `cpython_LICENSE.txt` (fetched 2026-10-29)
- Quote (saved line 65):
> Python Software Foundation License Version 2.

### S3.19 xmldiff API: ratio_mode and F, and what each does

- Used for: the two settings that decide how readily two nodes are paired; the default is chosen on measurements (metrics.md section 7)
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/docs/source/api.rst
- Saved copy: `xmldiff_api.rst.txt` (fetched 2026-10-29)
- Quote (saved line 56):
> Set the value high, and you will see more nodes inserted and deleted instead of being updated.
- Quote (saved line 57):
> Set the value low, and you will get more updates instead of inserts and deletes.
- Quote (saved line 68):
> The choices are ``'accurate'``, ``'fast'`` and ``'faster'``.
- Quote (saved line 73):
> Using ``'accurate'`` will be significantly slower,

### S3.20 xmldiff diff.py: ratio_mode accurate is difflib ratio()

- Used for: RatioMode.ACCURATE
- URL: https://raw.githubusercontent.com/Shoobx/xmldiff/master/xmldiff/diff.py
- Saved copy: `xmldiff_diff.py.txt` (fetched 2026-10-29)
- Quote (upstream line 35):
> elif ratio_mode == "accurate":
- Quote (upstream line 36):
> self._sequence_ratio = self._sequencematcher.ratio

### S3.21 Python difflib.py (CPython v3.12.0): ratio() and the longest matching block

- Used for: AccurateRatio.java is a port
- URL: https://raw.githubusercontent.com/python/cpython/v3.12.0/Lib/difflib.py
- Saved copy: `cpython_3.12.0_difflib.py.txt` (fetched 2026-10-29)
- Quote (upstream line 266):
> def __chain_b(self):
- Quote (upstream line 297):
> if self.autojunk and n >= 200:
- Quote (upstream line 305):
> def find_longest_match(self, alo=0, ahi=None, blo=0, bhi=None):
- Quote (upstream line 450):
> queue = [(0, la, 0, lb)]
- Quote (upstream line 619):
> matches = sum(triple[-1] for triple in self.get_matching_blocks())

### Rejected candidates (evidence for ADR 0001, section Alternatives considered)

### S3.10 Position only: sequence comparison

- Used for: a library default compares by position; an insertion shifts every later row
- URL: https://raw.githubusercontent.com/seperman/deepdiff/master/deepdiff/diff.py
- Saved copy: `deepdiff_diff.py.txt` (fetched 2026-10-29)
- Quote (upstream line 873):
> This will compare in sequence order.

### S3.11 Position only: BOM tool that offers a sequence index

- Used for: another tool lets the user pick part number or sequence as the index
- URL: https://raw.githubusercontent.com/polyfacet/aras-bom-compare/fc215453df6c61762cd3c01ebd134e378e806fb8/README.md
- Saved copy: `aras-bom-compare_README.md.txt` (fetched 2026-10-29)
- Quote (saved line 17):
> - Compare using different indexes (i.e. by Part Number/Sequence)

### S3.12 Names only: candidate counts

- Used for: fuzzy matching on names alone, and the effect of adding structure
- URL: https://arxiv.org/pdf/2411.10384
- Saved copy: `bom_comparing_2024.txt` (fetched 2026-10-29)
- Quote (saved line 511):
> useful because it flagged 743 possible matches which is
- Quote (saved line 474):
> the structure reduced the number of possible matches by 880

### S3.13 Zhang-Shasha: ordered trees, three edit operations

- Used for: ordered tree edit distance has no move operation
- URL: https://api.crossref.org/works/10.1137/0218082
- Saved copy: `zhang_shasha1989_abstract.txt` (fetched 2026-10-29)
- Quote (saved line 8):
> Ordered labeled trees are trees in which the left-to-right order among siblings is significant.
- Quote (saved line 8):
> weighted number of edit operations (insert, delete, and modify) to transform one tree to another

### S3.14 Zhang-Shasha: worst-case cost

- Used for: cost of the optimal algorithm
- URL: https://arxiv.org/pdf/cs/0604037
- Saved copy: `demaine2007_arxiv.txt` (fetched 2026-10-29)
- Quote (saved line 123):
> algorithm runs in O(n2m2) = O(n4) time.

### S3.15 GumTree: two stages, detects moves, heuristic

- Used for: mechanism of the two-stage alternative
- URL: https://arxiv.org/pdf/2011.10268
- Saved copy: `astdiff_hyperparam_2020.txt` (fetched 2026-10-29)
- Quote (saved line 263):
> isomorphic subtrees of decreasing height or size. Mappings
- Quote (saved line 270):
> include a large number of common anchors. Whenever a
- Quote (saved line 237):
> GumTree) are all based on heuristics because this problem

### S3.16 GumTree: moves are detected

- Used for: GumTree reports moves separately
- URL: https://raw.githubusercontent.com/GumTreeDiff/gumtree/main/README.md
- Saved copy: `gumtree_readme.txt` (fetched 2026-10-29)
- Quote (saved line 10):
> it can detect moved or renamed elements in addition of deleted and inserted code.

### S3.17 GumTree: licence is LGPL, so its code is not copied

- Used for: only the mechanism above is used; no GumTree code is copied
- URL: https://raw.githubusercontent.com/GumTreeDiff/gumtree/main/core/src/main/java/com/github/gumtreediff/matchers/heuristic/gt/GreedySubtreeMatcher.java
- Saved copy: `gumtree_src_GreedySubtreeMatcher.txt` (fetched 2026-10-29)
- Quote (saved line 7):
> it under the terms of the GNU Lesser General Public License as published by

### S3.18 X-Diff: unordered model, same-signature rule, no move

- Used for: X-Diff is for unordered trees and has no move
- URL: https://research.cs.wisc.edu/niagara/papers/xdiff.pdf
- Saved copy: `xdiff_wang2003.txt` (fetched 2026-10-29)
- Quote (saved line 23):
> ancestor relationships are significant) is more suitable for
- Quote (saved line 483):
> X-Diff only matches nodes that have the same signature.
- Quote (saved line 400):
> “move” operations are not necessary in the unordered


## 4. Duplicates, order, categories

### S4.1 ERPNext pull request 60755: duplicate rows and reordering

- Used for: same-key rows are paired one by one (identical rows first, then in order of appearance); reordering is not a change
- URL: https://github.com/frappe/erpnext/pull/60755
- Saved copy: `erpnext_pr_60755.txt` (fetched 2026-10-29)
- Quote (saved line 9):
> retaining only the last row for each identifier
- Quote (saved line 34):
> Reordering otherwise unchanged rows should not produce false changes.
- Quote (saved line 34):
> Report each added, removed, or changed occurrence separately.

### S4.2 InvenTree (MIT): duplicate rows get an occurrence suffix and are paired in order

- Used for: pairing same-key rows in order of appearance
- URL: https://raw.githubusercontent.com/inventree/InvenTree/24ce51c5ca335fc3c463a67299b7fda815f2e77b/src/frontend/src/pages/part/bom/BomCompare.tsx
- Saved copy: `inventree_BomCompare.tsx.txt` (fetched 2026-10-29)
- Quote (upstream line 286):
> subPartId += '_dup';
- Quote (upstream line 312):
> const match = rows.find((row) => row.key == subPartId);
- Quote (upstream line 351):
> return nameA.localeCompare(nameB);

### S4.3 Odoo pull request 91494: duplicate lines cannot be told apart

- Used for: scenario test 3
- URL: https://github.com/odoo/odoo/pull/91494
- Saved copy: `odoo_pr_91494.txt` (fetched 2026-10-29)
- Quote (saved line 23):
> and then we compare the BOM lines of the two BOMs if they are identical (product, qty and operation)
- Quote (saved line 55):
> we cannot check which line exactly was changed.

### S4.4 InvenTree issue 12445: an order-sensitive check value

- Used for: scenario test 4
- URL: https://github.com/inventree/InvenTree/issues/12445
- Saved copy: `inventree_issue_12445.txt` (fetched 2026-10-29)
- Quote (saved line 13):
> Because the hash is an order-sensitive MD5 of the concatenated per-line hashes,

### S4.5 BOMs are unordered

- Used for: bill rows are compared as an unordered collection; process steps are ordered
- URL: https://arxiv.org/pdf/2411.10384
- Saved copy: `bom_comparing_2024.txt` (fetched 2026-10-29)
- Quote (saved line 271):
> BOMs are unordered.

### S4.6 SAP routing comparison symbols

- Used for: names and symbols of the categories (= same, not-equal changed, A only in A, B only in B)
- URL: https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/9c4986bda35f4840ae438960ffbef64d/3c389337b2554ec1b2e2e22cb9a92555.html?locale=en-US&state=PRODUCTION&version=2025.002
- Saved copy: `sap_s4_comparing_routings.txt` (fetched 2026-10-29)
- Quote (saved line 68):
> The is equal to symbol shows that the object is identical in
- Quote (saved line 70):
> The is not equal to symbol shows that there are differences
- Quote (saved line 72):
> An A means that this object only exists in version A.
- Quote (saved line 73):
> A B means that this object only exists in version B.

### S4.7 Oracle structure comparison filter and Item Sequence

- Used for: filter names All, Differences, Added, Changes, Removed, Unchanged; sequence compared as an attribute, no separate category; the comparison has an Excel export
- URL: https://docs.oracle.com/en/cloud/saas/supply-chain-and-manufacturing/26d/faupd/compare-structures-new.html
- Saved copy: `oracle_fusion_compare_structures.txt` (fetched 2026-10-29)
- Quote (saved line 41):
> such as All, Differences, Added, Changes,
- Quote (saved line 67):
> the Item Sequence.
- Quote (saved line 35):
> both the comparison UI and its Excel


## 5. Change generator, measurement and choice of the default (docs/validation/metrics.md)

### S5.1 X-Diff paper, section 5: the change generator

- Used for: insert/delete/update at random on each level; the share of changed nodes is a parameter; the paper's generator does not permute
- URL: https://research.cs.wisc.edu/niagara/papers/xdiff.pdf
- Saved copy: `xdiff_wang2003.txt` (fetched 2026-10-29)
- Quote (saved line 1016):
> randomly at each level. The program takes
- Quote (saved line 1017):
> a parameter, the percentage of nodes being changed.
- Quote (saved line 1022):
> does not permute the order of any nodes

### S5.2 Edit operation names

- Used for: insert, remove, update, move
- URL: https://arxiv.org/pdf/2011.10268
- Saved copy: `astdiff_hyperparam_2020.txt` (fetched 2026-10-29)
- Quote (saved line 153):
> operations are typically typed as {insert, remove, update,

### S5.3 Precision and recall

- Used for: definitions in metrics.md
- URL: https://raw.githubusercontent.com/scikit-learn/scikit-learn/1.5.2/sklearn/metrics/_classification.py
- Saved copy: `sklearn_1.5.2_classification.py.txt` (fetched 2026-10-29)
- Quote (upstream line 1628):
> The precision is the ratio ``tp / (tp + fp)`` where ``tp`` is the number of
- Quote (upstream line 1633):
> The recall is the ratio ``tp / (tp + fn)`` where ``tp`` is the number of

### S5.5 scikit-learn user guide, Cross-validation: choose on one part of the data, report on another

- Used for: tuning seeds 1 to 20, held-out seeds 21 to 40
- URL: https://scikit-learn.org/stable/modules/cross_validation.html
- Saved copy: `sklearn_cross_validation_user_guide.txt` (fetched 2026-10-29)
- Quote (saved line 124):
> Learning the parameters of a prediction function and testing it on the
- Quote (saved line 125):
> same data is a methodological mistake: a model that would just repeat
- Quote (saved line 131):
> to hold out part of the available data as a test set X_test, y_test.

### S5.4 BSD-3-Clause licence of scikit-learn (definitions only; no code copied)

- Used for: provenance of the sentences above
- URL: https://raw.githubusercontent.com/scikit-learn/scikit-learn/1.5.2/COPYING
- Saved copy: `sklearn_1.5.2_COPYING.txt` (fetched 2026-10-29)
- Quote (saved line 3):
> BSD 3-Clause License


## 6. Architecture decision record format

### S6.1 Nygard, Documenting Architecture Decisions

- Used for: ADR parts: Context, Decision, Consequences (Status is kept); 'Alternatives considered' is added to it
- URL: https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions
- Saved copy: `nygard_documenting_architecture_decisions.txt` (fetched 2026-10-29)
- Quote (saved line 89):
> Context This section describes the forces at play, including
- Quote (saved line 93):
> Decision This section describes our response to these forces. It
- Quote (saved line 99):
> Consequences This section describes the resulting context, after


## 7. Bundled B2MML schema files

- Repository: https://github.com/MESAInternational/B2MML-BatchML
- Commit: `65cc66534343c86a1ce953561e121f906a0a0cbe` (2023-07-11, head of master on 2026-10-29)
- Saved copy: `b2mml_commit_65cc665.txt` (fetched 2026-10-29): the commit and the file list returned by the GitHub API
- Every file was fetched from `https://raw.githubusercontent.com/MESAInternational/B2MML-BatchML/65cc66534343c86a1ce953561e121f906a0a0cbe/Schema/<name>` and its git blob SHA-1 equals the SHA-1 given by the GitHub contents API for that commit (compared when the files were fetched). The files are stored in `src/main/resources/b2mml/` without change; `.gitattributes` switches line-ending conversion off for them so that the hashes stay valid.
- Not bundled: `AllSchemas.json` (the unchecked JSON form).

| file | bytes | sha256 |
|---|---:|---|
| AllSchemas.xsd | 4154 | ec61ce5084574354c1d10bc06fc77d536e50d55c2fc910ca90e275f7748eb85b |
| B2MML-AllExtensions.xsd | 3356 | 7a279dfb72d797fbea15db93b80faf3fd70ea2257784627cf4639a7b274efc43 |
| B2MML-Common.xsd | 102769 | 7b439e92168080b60ad544408f7a547da79de7ff223f11d40cd1b70a74a156e0 |
| B2MML-CommonExtensions.xsd | 14264 | 1a3de15a0dca64974b32dc3b11af1605edb01a569dea2a5a32436b4ad6d17065 |
| B2MML-ConfirmBOD.xsd | 3712 | c0e47456c88cb1263900dd548b201f2d3b15f5dfdd2cd954cb45eb40e177d5b7 |
| B2MML-CoreComponents.xsd | 10140 | ebaa8edcf00c6f218c4b096bd2bc46e4789440db4fd378ec72af6c6402eb584a |
| B2MML-Equipment.xsd | 35240 | d6bd1109498f64e2e1226a129405bd419bcc7e850314c6f4fdf783a734bb8182 |
| B2MML-ErrorMessage.xsd | 12019 | bd3d3b91611c81e14e1266f0c97f182a170ba07d66b04319a1390c46761fb881 |
| B2MML-Extensions.xsd | 34854 | b26a0e1bbfa515b8579458dd362993a99972a994d859b42caa51bf00d5f347ea |
| B2MML-InformationObject.xsd | 13298 | f322c71ca2ffc15322fd1ab6bac13e962ee3300779541f85f86f45b06a4e11ab |
| B2MML-MasterDataProfile.xsd | 16165 | 57e0a953ad7b785987635d3d070048d9058d64d7f409cffa3c08dfc30551fff8 |
| B2MML-Material.xsd | 60332 | d4e88b949be2689cd87fcb84f32cf261b1ffa6bcb80c139e774658c3ee660590 |
| B2MML-OperationalLocation.xsd | 36987 | fda14696a063355b09e01ef7235dea7a3f249d0362f600dbd4d9bb7ba3ca978a |
| B2MML-OperationsCapability.xsd | 29585 | 05271e4cfa88eb09e36a9a00af2b6bfe16c71bcfb13a5a2a109ffd14c692bf18 |
| B2MML-OperationsDefinition.xsd | 40955 | 7c38b6303cb20ad99f01aa692d3df924e71af09a76f101a1336dc67993db446b |
| B2MML-OperationsEvent.xsd | 49176 | e5f0b8aee392bfd5c0b2a309ecc46b75087413f68427a6b7241c0e7efeea778a |
| B2MML-OperationsPerformance.xsd | 15530 | 9c1f981794473b53250862d86ea133695459eb6a8973df9ad2308887dd9b31c6 |
| B2MML-OperationsPerformanceTypes.xsd | 25069 | 933486829acd216cee35ab023a715c06719b156c399c657158e8f40dceeaca6f |
| B2MML-OperationsSchedule.xsd | 18603 | a613cd3a4f5e078eb1b538401fad5ccedcc59a6bea20227d8f77c47aee354829 |
| B2MML-OperationsTest.xsd | 39947 | 6bb69d4fd75a7977d33228b688864f038ad89a8171dc74ebfdc2a20e2ffea0eb |
| B2MML-Personnel.xsd | 34791 | 4029233ac0382ef9a808fb6d5e4786faf498c6c4be59a2ed0f0ebeaccdd9f655 |
| B2MML-PhysicalAsset.xsd | 36749 | 92fd4f41e3a472afe414460a9b196ba6ff8e25ee26a0045f9c19c9807196e10d |
| B2MML-ProcessSegment.xsd | 41674 | 63eae4d455de992f30afd76e510abb2af4b2d5d7b4f512a94f01995c2715253a |
| B2MML-ResourceRelationshipNetwork.xsd | 40088 | bb4ff87bbe73a21a1018d7374ed6c3adeb54c33fdc5f41c5ee33bb14198e078e |
| B2MML-TransactionProfile.xsd | 15996 | 7f34141b8a3e6d5b63b73f08097bb747021dc7605e27def7a69cee578e46baf4 |
| B2MML-WorkAlert.xsd | 33880 | a8b1ae88e210898d865a6ed70306b77fb4fd50182947b2d504bf0144f7294e81 |
| B2MML-WorkCalendar.xsd | 40492 | 53814967a0a95f5418c408544f9fa6440f5a5bccba78acc59739c3bdb0cc1e5b |
| B2MML-WorkCapability.xsd | 33754 | cddf71e1e18fed04477649a612a6e69c8ed5a0af61c47ee17d2914046e17fcf8 |
| B2MML-WorkDefinition.xsd | 33212 | 0955dea40d8c838519f67fc4bdee36f77b1c66e45e5becacf61cd0a52a6ce931 |
| B2MML-WorkPerformance.xsd | 44626 | ba2797ceba873fd701e8caf9b7b91eb44397c67883d8c27a3d0e76cae9df6560 |
| B2MML-WorkRecord.xsd | 27600 | 898a93cae8a9bdd4dbc225fbcbb1c1ca71424d17e38438360163782c35f767ab |
| B2MML-WorkSchedule.xsd | 27866 | 8573dad4ceaab59eae10db80de96fba0ca38f912972286a4300e0d49005cd457 |
| B2MML-WorkflowSpecification.xsd | 53881 | 0e63b3b19e55178e1e9e5d0c4caf33b1321d16bd890d24d1092b5d138e67a6ab |
| BatchML-BatchInformation.xsd | 59702 | 520da59b546d15a85578a7db7ad5211beada4b7f26388bdbcea0c2b6832e218e |
| BatchML-BatchInformationExtensions.xsd | 9774 | 37d9918ade0eb514b0982259baa31428e30bf18cad9bea40442958ad94192540 |
| BatchML-BatchProductionRecord.xsd | 42096 | d4aaba923f1496e98865c43900da930737d7ec7a06467a992f2cab2cb01ae67f |
| BatchML-BatchProductionRecordExtensions.xsd | 3180 | de82bac502201ca665ddc5af3ef046066e8d35f6265faa37c709831959896aa5 |
| BatchML-GeneralRecipe.xsd | 54249 | 558f190f659862e5e43368b617a0b55f73ae7b76deaadca5b8ee71a95a9c4232 |
| BatchML-GeneralRecipeExtensions.xsd | 5375 | 119b2c2f42a606ddcff3e61cf6096def9ca33d3bd379fcfda6cbe0f04e3c7352 |


## 8. Test scenarios taken from failures of other tools (OssFailureScenariosTest)

| test method | situation | source entry |
|---|---|---|
| `erpnextPr60755_sameRowTwice_removingOneRowIsReported` | a part appears in two rows; one row is deleted | S4.1 |
| `erpnextPr60755_sameRowTwice_addingAThirdRowIsReported` | a part appears in two rows; a third is added | S4.1 |
| `erpnextPr60755_sameRowTwice_eachChangedOccurrenceIsReportedSeparately` | both rows of one part change | S4.1 |
| `erpnextPr60755_reorderingDuplicateRowsProducesNoFalseChange` | rows of a part are reordered | S4.1 |
| `erpnextPr60755_reorderedUnchangedProcessIsMovedNotChanged` | an unchanged step changes position; this tool reports "moved", not "changed" | S4.1 |
| `odooPr91494_duplicateOperationIdsProduceNoFalseChange` | two steps share one ID | S4.3 |
| `odooPr91494_duplicateOperationIdsWithOneChangedStillPairByContent` | two steps share one ID and one row changes | S4.3 |
| `odooPr91494_duplicateOperationIdsInAnotherOrderArePairedByContentNotByIdOrder` | two steps share one ID and swap places | S4.3 |
| `inventreeIssue12445_reorderedBillRowsAreSame` | the rows of a bill are reordered | S4.4 |


## 9. Author's decisions (no source)

These are choices made by the author of this repository. They have no source, or the source covers only part of the choice (stated in each item). They are listed here so that nothing without a source looks like it has one.

| no. | decision | what is sourced, what is not |
|---|---|---|
| D1 | The part key of a bill item is the text of its `MaterialSpecificationID` element (several values are joined with a vertical bar). The sample data writes the part number there. | Sourced: the schema has `MaterialSpecificationID` in the bill item (S1.4). Not sourced: using it as the part number. `MaterialDefinitionID` is the more usual name for a part number, but the bill item has no such element (0 hits in the saved schema), so it is not used. |
| D2 | A segment's bill is the `OperationsMaterialBill` whose `ID` equals the segment's first `OperationsMaterialBillID`. A missing bill gives a warning and no items. | Sourced: the two element names (S1.4). The lookup rule is the author's. |
| D3 | The position number of a segment is a `ParameterSpecification` with `ID` = `Indice`, a `Value/ValueString` (digits) and `DataType` = `integer`. Segments are ordered by that value (numbers by value, other text as text); when any segment has none, the document order is used. The value is the position key and is not a compared field. | Sourced: the name `Indice`, the digits `0010`, and the ID + Value shape (S1.5, S1.6); the saved schema has no property element in an operations segment, so the segment's parameter element is the nearest place with the same shape. Not sourced: that this is the correct place in a real plant's data, the sorting rule, and leaving the value out of the comparison. |
| D4 | Compared fields: the simple child elements listed in `B2mmlReader.SEG_SIMPLE` and `ITEM_SIMPLE`, every other `ParameterSpecification` (value, `DataType`, `UnitOfMeasure`) and the first `Quantity` (`QuantityString`, `DataType`, `UnitOfMeasure`) of an item. Values are compared as text ("4" and "4.0" differ). The item line `ID` is a compared field. | Not sourced. |
| D5 | Only the first `OperationsDefinition` is read. Nested segments, nested bill items, dependencies, equipment/personnel specifications and any other element are not compared; each kind found is reported as a warning. | Not sourced. |
| D6 | A segment ID decides alone only when it occurs exactly once on each side. Other segments, including renumbered ones, are paired by similarity. A bill item that has a part key is never paired with an item that has a different key, however similar. | Sourced: the first half follows S1.7; the second half is the xmldiff sentence in S3.4 applied to items. Not sourced: relaxing the rule for segments (needed to detect renumbered steps). |
| D7 | Rows with the same part key are paired in two passes: identical rows first, then the rest in order of appearance. | Sourced: the expectations (S4.1) and the InvenTree occurrence pairing (S4.2). The two-pass procedure itself is the author's; the ERPNext fix was not read (GPL). |
| D8 | The text compared for the own-text ratio is the kind label followed by `name:value` pairs sorted by name. | Sourced: the construction of `node_text` in xmldiff (S3.1 and docs/adr/0001-matching.md). Not sourced: flattening B2MML child elements into that text. A trial with values only (no field names) gave the same measurement table and was not kept. |
| D9 | The status of a segment (same or changed) depends on its own fields only. Bill item differences are separate rows. The items of an added (removed) segment are added (removed). | Not sourced. |
| D10 | "Moved" is a flag on a paired segment that is not on the longest common subsequence of the paired order; it can be set together with same or changed. Its name and the symbol ↕ are new. | Sourced: the LCS and the move rule (S3.3). The reasons why it is a separate category are in ADR 0001. SAP and Oracle (S4.6, S4.7) have no such category. |
| D11 | Input limits: 8 MiB of bytes, element depth 64, 200,000 elements. | Sourced: the advice to limit (S2.1, last quote). The numbers are the author's; the sample files are about 10 KB. |
| D12 | Change generator: segment operations are insert, remove, update, move, renumber (equal chance); item operations are insert, remove, update, and one in four inserts is a duplicate of an existing part (with a different quantity). Each node is chosen at most once; the number of operations is `max(1, round(share x (segments + items)))`; a move is at least 2 positions long; new IDs are `OP-9nn` and `LN-nnnnn`; a new segment has 0 to 3 items; positions are renumbered 10, 20, ... in the second revision. | Sourced: insert/delete/update at random with the share as a parameter (S5.1), and the move operation name (S5.2). Not sourced: `renumber`, the duplicate insert, the rest of this list. The minimum move length of 2 is there because moving a step by one position is the same result as moving its neighbour. |
| D13 | Synthetic base revision: 30 segments, 0 to 6 bill items each, 6 per cent of items followed by a duplicate row, 80 invented parts, random invented names. Tuning seeds 1 to 20, held-out seeds 21 to 40; the operation seed is `seed x 1000 + percent`. | Sourced: only the idea of separate seeds for choosing and for reporting (S5.5). The rest is not sourced. |
| D14 | Measurement: sums over the 20 seeds of a set; an object's identity is the number the generator gave it; a wrong pair counts as one false positive and one false negative; "exact run" means every category at both levels equals the truth. | Sourced: the precision and recall formulas (S5.3). Not sourced: the rest. No target value was set. |
| D15 | Sample data: the fictional company and product, the 12 steps, and which changes revision B contains. | Not sourced (invented data, marked as fictional in the file header). |
| D16 | The command line has `--filter` (with the Oracle names and `Moved`), `--validate`, `--f` and `--ratio`; output is UTF-8; exit code 0 on success and 2 on a usage or input error. | Sourced: the filter names (S4.7). Not sourced: the rest. |
| D17 | **Default matcher setting: ratio mode `fast`, F = 0.7** (xmldiff's own default is `fast`, 0.5). Chosen by the rule in metrics.md section 7 (written before the grid was run) from the grid on the tuning seeds 1 to 20: the lowest segment-level error count is 0, reached by `fast` and `accurate` at F = 0.7 and F = 0.8; the tie rule (fast first, then lower F) gives `fast`, 0.7. At F = 0.5 and 0.6 the count is 59 (fast) and 60 (accurate); at 0.9 it is 12. Evidence is in results.md part 1; the held-out result is in part 2. | Sourced: that `F` and `ratio_mode` are the two settings and which way `F` pushes the result ("you will see more nodes inserted and deleted instead of being updated", S3.19), and the xmldiff defaults (S3.1, S3.19). Not sourced: the numeric choice itself. It depends on the invented base data and on the rule; with 0 errors on two neighbouring values the grid cannot say which of 0.7 and 0.8 is better, and other data may need another value. |

| D18 | Colours. Changed row = `FFFFE0` ("lightyellow"), changed cell = `FFFF00` ("yellow"); added row `E2F5E2`, removed row `FBE3E8`, moved-only row `E3EEFB`. The same colours are in `app.css` and in `XlsxExport`. | Sourced: the colour names for a changed row and a changed cell (S11.7), "differences are highlighted in yellow on the cell" (S11.10c), and the names "green" and "pink" for added and removed rows (S11.7; in the saved file they are comment lines that show how the colours can be changed). Not sourced: the RGB values that belong to those names (the usual web colour values), the pale shades of green, pink and blue, and the colour for moved rows. |
| D19 | Server details: default port 8080; 4 worker threads; the last 16 comparisons are kept in memory under a random 128-bit id and then dropped; a form post has at most 8 parts and a header block of at most 4096 bytes per part; a body above the 8 MiB limit is read and thrown away up to 16 MiB so that the browser still gets the answer 413; a successful POST answers 303 to the result page. | Sourced: the 8 MiB value is D11; the boundary length limit comes from RFC 2046 (S11.3b). Not sourced: everything else in this row. |
| D20 | What the screen shows: Japanese words; the filter names in Japanese (すべて, 差異あり, 追加, 変更, 削除, 変更なし, 入れ替え); the symbols = ≠ A B ↕; the columns of the text report plus a category column (and a step column on the bill table); steps in the order of revision B and removed steps last; a filter is the query `?filter=` and is applied by the server; "previous / next difference" does not wrap around, and clicking a difference row sets the current position; one button for the sample files. | Sourced: the filter names (S4.7), the symbols (S4.6), the idea of previous / next buttons (S11.10a, S11.10b), yellow cells (S11.10c). Not sourced: the wording, the layout, the row order, the behaviour of the buttons at the ends. |
| D21 | Excel layout: sheet names まとめ / 工程 / 部品; columns (category word, moved mark, A#, B#, A id, B id, name, what changed; the bill sheet has the step in place of the moved mark); the summary sheet (files, setting, counts per category, colour legend); fixed column widths; text longer than 32767 characters is cut at 32767; the first row of the two data sheets is frozen and has the filter arrows. | Sourced: that Excel has freeze panes and AutoFilter and that POI can set them (S11.8, S11.8b, S11.8c); an Excel report of changes (S11.10d); an exported list (S11.10b). Not sourced: the layout, the widths and the 32767 limit (it is the size of an Excel cell; no saved page states it, POI refuses longer text). |
| D22 | Response headers and host check: `Content-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`, a fixed download name; the Host header must be `127.0.0.1:<port>` or `localhost:<port>`; when `Sec-Fetch-Site` is sent it must be `same-origin`. | Sourced: Origin and Referer checks, refusing `Origin: null`, rejecting a cross-site `Sec-Fetch-Site` (S11.2); a CSP as an additional layer (S11.1); validating the Origin and binding to localhost for a local server (S11.4). Not sourced: the exact directives of the policy, `nosniff`, `no-store`, and the list of host names. |
| D23 | Text from the files is always written as a string cell. When its first character is one of `= + - @`, tab, carriage return, line feed or their full-width forms, the cell also gets Excel's quote-prefix mark; the stored text is not changed (no apostrophe, no tab is added). | Sourced: the list of characters (S11.5) and the existence of the quote-prefix mark (S11.6). Not sourced: that a string cell of an .xlsx file is not read as a formula (the OWASP page is about CSV files, N9), and the decision to add the mark. |
| D24 | The "could not find a logging provider" line is removed with `log4j2.component.properties` that selects the Simple Logger of log4j-api, not by adding log4j-core. | Sourced: the method (S11.9). Not sourced: choosing the Simple Logger over log4j-core (the page offers both; log4j-core would be a new library). |
| D25 | log4j-api is pinned to 2.25.5, the smallest version that OSV lists as fixed for GHSA-qv9r-c865-cp47 (2.26.1 is also listed). POI stays on 5.5.1 although 6.0.0 exists. | Sourced: the fixed versions (S11.13) and the OSV result for 5.5.1 and its compile dependencies (S11.13b); POI 6.0.0 released on 22 October 2026 (S11.12). Not sourced: choosing 2.25.5 over 2.26.1 and 5.5.1 over 6.0.0 (5.5.1 is the version that was checked and is in the local Maven repository). |
| D26 | `scripts/run.ps1` and `scripts/run.sh` list the 13 jars of the compile classpath that Maven resolves for poi-ooxml 5.5.1 with the log4j-api pin (as listed by `mvn -X`, 2026-10-29). `ProjectFilesTest` checks that both scripts list the same files, that each is in `~/.m2`, and that the POI and log4j versions equal the pom. | Not sourced (a build detail). |


## 10. Searched, not found

Each item says what was needed, where it was searched, and what was done instead.

- **N1. A public sample with two revisions of a B2MML process plan and material bill.** Searched: the whole tree of the MESA repository and its three example archives; GitHub repository search for "B2MML" (26 repositories) and the trees of five related repositories; Zenodo; Hugging Face datasets; GitLab and Codeberg; NIST data. Found: only single-revision examples. Not read: the examples of the `krennt/b2mml_workflow` repository (GitHub API rate limit). Done instead: invented data (Author's decision D15) written in the element names of the current schema and validated against it.
- **N2. A `MaterialDefinitionID` element inside a bill item.** Searched: the saved `B2MML-OperationsDefinition.xsd` (0 hits). Done instead: Author's decision D1.
- **N3. A property element on an operations segment to carry the position number.** Searched: the same file (0 hits for `Property`). Done instead: Author's decision D3.
- **N4. A public document in which a PLM or ERP product reports "same content, different position" as its own category.** Read: SAP, Oracle, Windchill, OpenBOM documentation pages. Not readable: Siemens Teamcenter and Dassault pages (JavaScript or login), Autodesk Fusion Manage (HTTP 403), Duro (no connection). So "none found" covers only what was read. Done instead: reasons in ADR 0001, Decision, last paragraph.
- **N5. Numbers for the input limits.** Searched: the saved OWASP page (it asks for limits and gives no values). Done instead: Author's decision D11.
- **N6. The procedure inside ERPNext's fix for duplicate rows.** Not searched, on purpose: the code is GPL-3.0 and is not copied. Only the text of the pull request was used (S4.1). Done instead: Author's decision D7.
- **N7. The licence of the OWASP cheat sheet.** Not in the saved copy of the page. No text of the page is copied into the repository; only quotes of a few words are used in this file.
- **N8. A published rule for which segment counts as "moved" when several orders have the same longest common subsequence** (for example swapping two neighbours). Not searched. This tool takes what the ported algorithm returns; the effect is described in `docs/validation/metrics.md`.
- **N9. A published statement on formula injection for the string cells of an .xlsx file.** Searched: the saved OWASP CSV Injection page (it is about CSV files and lists the characters; it says nothing about .xlsx) and the saved POI `CellStyle` page (quote prefix). Done instead: Author's decision D23 and ADR 0004.
- **N10. A recommended value for the largest upload.** Searched: the saved OWASP XSS and CSRF pages (no number). Done instead: the value of D11 is used for the request body too (D19).
- **N11. A measured comparison of the base for the screen and of the way to write the Excel file.** The alternatives of ADR 0003 (Jakarta Servlet with a container, Spring Boot) and ADR 0004 (CSV, hand-written OOXML) were not installed or tried here: no such library is in the local Maven repository and the project uses no library other than POI. The reasons in the ADRs come from the quoted sentences (S11.5, S11.11, S11.11b, S11.12) and from the rule of one new library; they are not measurements.
- **N12. A published test of the DNS-rebinding check for the JDK HttpServer.** Not found. `ServerTest.aRequestForAnotherHostNameIsRefused` sends a request with another Host header over a plain socket.
- **N13. A screenshot of the Excel file.** Not made: Excel is not installed on the PC used for the checks. The file was opened and read back with POI (README, "How it was checked").
- **N14. Open-source tools that compare only process plans (routings), and small bill-comparison repositories.** The search for open-source tools used the words "bom diff", "bom compare" and "bom comparison"; the word "routing" was not used. The GitHub API rate limit stopped the reading of the contents of small repositories (for example BomComparer). So the statement in the README that no tool reports a moved step as its own category covers only the pages and code that were read. Done instead: the README statement says "in the pages and code that were read".

## 11. Screen, upload and Excel export (docs/adr/0003 to 0005)

Every quote below is a substring of one line of its saved copy; the checks are listed in the README.

### S11.1 OWASP XSS Prevention Cheat Sheet: encode text for an HTML context

- Used for: Html.esc; Pages (text from files only as element text); ADR 0005
- URL: https://raw.githubusercontent.com/OWASP/CheatSheetSeries/master/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.md
- Saved copy: `owasp_xss_prevention_cheatsheet.txt` (fetched 2026-10-29)
- Quote (saved line 59):
> use HTML entity encoding for that variable.
- Quote (saved line 271):
> Convert `&` to `&amp;`, Convert `<` to `&lt;`, Convert `>` to `&gt;`, Convert `"` to `&quot;`, Convert `'` to `&#x27`
- Quote (saved line 157):
> Don't place variables into dangerous contexts as even with output encoding, it will not prevent an XSS attack fully.
- Quote (saved line 83):
> Always surround attribute values with double
- Quote (saved line 218):
> Use a CSP as an additional layer of defense
- Quote (saved line 218):
> it should not be your primary defense mechanism

### S11.2 OWASP CSRF Prevention Cheat Sheet: verify the origin with standard headers

- Used for: OriginGuard.sourceAllowed; Server.compare; ADR 0005
- URL: https://raw.githubusercontent.com/OWASP/CheatSheetSeries/master/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.md
- Saved copy: `owasp_csrf_prevention_cheatsheet.txt` (fetched 2026-10-29)
- Quote (saved line 409):
> If the Origin header is present, verify that its value matches the target origin.
- Quote (saved line 413):
> If the Origin header is not present, verify that the hostname in the Referer header matches the target origin.
- Quote (saved line 415):
> does not pass your origin check
- Quote (saved line 417):
> Missing headers and `Origin: null` are not evidence of a same-origin request.
- Quote (saved line 425):
> **Configure your application to simply know its target origin:**
- Quote (saved line 150):
> By default, reject non-safe methods (POST / PUT / PATCH / DELETE) when `Sec-Fetch-Site: cross-site`.
- Quote (saved line 434):
> so do not allowlist the literal `null` value.
- Quote (saved line 413):
> This method of CSRF mitigation is also commonly used with unauthenticated requests
- Quote (saved line 238):
> These "simple" requests introduce risk of CSRF because browsers permit them to be sent to any origin.

### S11.3 multipart/form-data (RFC 7578)

- Used for: Multipart.parse
- URL: https://www.rfc-editor.org/rfc/rfc7578.txt
- Saved copy: `rfc7578_multipart_form_data.txt` (fetched 2026-10-29)
- Quote (saved line 189):
> boundary delimiter, constructed using CRLF, "--", and the value of
- Quote (saved line 202):
> the original field name from the

### S11.3b RFC 2046: boundary, delimiter and close-delimiter

- Used for: Multipart.parse (boundary at most 70 characters; delimiter = CRLF + "--" + boundary)
- URL: https://www.rfc-editor.org/rfc/rfc2046.txt
- Saved copy: `rfc2046_multipart_boundary.txt` (fetched 2026-10-29)
- Quote (saved line 1210):
> boundary := 0*69<bchars> bcharsnospace
- Quote (saved line 1251):
> delimiter := CRLF dash-boundary
- Quote (saved line 1253):
> close-delimiter := delimiter "--"

### S11.4 A local server: bind to localhost only and validate the Origin header (DNS rebinding). Written for MCP servers; used here by analogy for any local server that a browser can reach

- Used for: Server constructor (loopback address); OriginGuard.hostAllowed
- URL: https://modelcontextprotocol.io/specification/2025-06-18/basic/transports
- Saved copy: `mcp_transports_origin.txt` (fetched 2026-10-29)
- Quote (saved line 103):
> Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks
- Quote (saved line 104):
> When running locally, servers SHOULD bind only to localhost (127.0.0.1) rather than all network interfaces (0.0.0.0)

### S11.4b JDK 17 HttpServer: what it is and how it is bound

- Used for: Server; ADR 0003
- URL: https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html
- Saved copy: `jdk17_httpserver_HttpServer.txt` (fetched 2026-10-29)
- Quote (saved line 37):
> This class implements a simple HTTP server.
- Quote (saved line 116):
> Create a HttpServer instance which will bind to the
- Quote (saved line 46):
> Any request for which no handler can be found is rejected with a 404 response.

### S11.5 OWASP CSV Injection: cells that start with = + - @ (and full-width forms) are read as formulas when a CSV is opened

- Used for: XlsxExport.startsLikeFormula; ADR 0004
- URL: https://raw.githubusercontent.com/OWASP/www-community/master/pages/attacks/CSV_Injection.md
- Saved copy: `owasp_csv_injection.txt` (fetched 2026-10-29)
- Quote (saved line 16):
> CSV Injection, also known as Formula Injection, occurs when websites
- Quote (saved line 20):
> is used to open a CSV, any cells starting with `=` will be interpreted
- Quote (saved line 39):
> - Equals to (`=`)
- Quote (saved line 40):
> - Plus (`+`)
- Quote (saved line 41):
> - Minus (`-`)
- Quote (saved line 42):
> - At (`@`)
- Quote (saved line 43):
> - Tab (`0x09`)
- Quote (saved line 44):
> - Carriage return (`0x0D`)
- Quote (saved line 45):
> - Line feed (`0x0A`)
- Quote (saved line 46):
> Full-width (double-byte) variants of formula-initiating characters such as
- Quote (saved line 48):
> (e.g., Japanese environments).
- Quote (saved line 58):
> * Prepend each cell field with a single quote
- Quote (saved line 91):
> There is no universal CSV sanitization strategy that is safe for all
- Quote (saved line 61):
> Note: The above techniques are not reliable in Microsoft Excel after

### S11.6 Apache POI: the quote-prefix mark on a cell style (Excel's own form of the leading apostrophe)

- Used for: XlsxExport.Styles.get (setQuotePrefixed); ADR 0004. The saved page is the Javadoc of the development version of POI; the same call is used here against 5.5.1 and compiles
- URL: https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/CellStyle.html
- Saved copy: `poi_apidocs_CellStyle.txt` (fetched 2026-10-29)
- Quote (saved line 186):
> Turn on or off "Quote Prefix" or "123 Prefix" for the style,
- Quote (saved line 187):
> which is used to tell Excel that the thing which looks like
- Quote (saved line 112):
> like prefixing the cell value with a ' in Excel

### S11.7 aras-bom-compare (MIT): colours of the comparison output

- Used for: XlsxExport colours; screen colours
- URL: https://raw.githubusercontent.com/polyfacet/aras-bom-compare/fc215453df6c61762cd3c01ebd134e378e806fb8/src/packages/hcaras/hc.bomcompare/Import/Method/HC_PartBomCompare_Generic.xml
- Saved copy: `aras_HC_PartBomCompare_Generic.xml.txt` (fetched 2026-10-29)
- Quote (saved line 85, upstream line 83):
> outputSettings.CellColors.ChangedRowColor = "lightyellow"
- Quote (saved line 86, upstream line 84):
> outputSettings.CellColors.ChangedCellColor = "yellow"
- Quote (saved line 89, upstream line 87):
> outputSettings.CellColors.AddedRowColor = "green"
- Quote (saved line 90, upstream line 88):
> outputSettings.CellColors.RemovedRowColor = "pink"

### S11.7b aras-bom-compare licence

- Used for: NOTICE
- URL: https://raw.githubusercontent.com/polyfacet/aras-bom-compare/fc215453df6c61762cd3c01ebd134e378e806fb8/LICENSE
- Saved copy: `aras_LICENSE.txt` (fetched 2026-10-29)
- Quote (saved line 3):
> MIT License
- Quote (saved line 5):
> Copyright (c) 2023 Tomas Andersson

### S11.8 Microsoft support: freeze panes in Excel

- Used for: XlsxExport.header (createFreezePane, setAutoFilter); ADR 0004
- URL: https://support.microsoft.com/en-us/office/freeze-panes-to-lock-rows-and-columns-dab2ffc9-020d-4026-8121-67dd25f2508f
- Saved copy: `ms_freeze_panes.txt` (fetched 2026-10-29)
- Quote (saved line 172):
> To keep an area of a worksheet visible while you scroll to another area of the worksheet

### S11.8b Microsoft support: AutoFilter

- Used for: same
- URL: https://support.microsoft.com/en-us/office/filter-data-in-a-range-or-table-01832226-31b5-4568-8806-38c37dcc180e
- Saved copy: `ms_filter_data.txt` (fetched 2026-10-29)
- Quote (saved line 172):
> Use AutoFilter or built-in comparison operators like

### S11.8c Apache POI Sheet API: freeze pane and filter

- Used for: same. The saved page is the Javadoc of the development version of POI; the calls are used here against 5.5.1 and compile
- URL: https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/Sheet.html
- Saved copy: `poi_apidocs_Sheet.txt` (fetched 2026-10-29)
- Quote (saved line 1086):
> Creates a split (freezepane). Any existing freezepane or split pane is overwritten.
- Quote (saved line 357):
> Enable filtering for a range of cells

### S11.9 Log4j manual: the "could not find a logging provider" line and how to remove it without a new library

- Used for: src/main/resources/log4j2.component.properties; README FAQ; ADR 0004
- URL: https://logging.apache.org/log4j/2.x/manual/installation.html
- Saved copy: `log4j_installation.txt` (fetched 2026-10-29)
- Quote (saved line 212):
> main ERROR Log4j API could not find a logging provider.
- Quote (saved line 208):
> implementation is embedded in the Log4j API and does not need any external dependency.
- Quote (saved line 213):
> To remove the warning and confirm that you want to use Simple Logger, add a
- Quote (saved line 217):
> log4j.provider = org.apache.logging.log4j.simple.internal.SimpleProvider

### S11.10a SAP routing comparison: move from change to change with Previous / Next

- Used for: buttons on the result page
- URL: https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/9c4986bda35f4840ae438960ffbef64d/3c389337b2554ec1b2e2e22cb9a92555.html?locale=en-US&state=PRODUCTION&version=2025.002
- Saved copy: `sap_s4_comparing_routings.txt` (fetched 2026-10-29)
- Quote (saved line 74):
> To quickly move from change to change, you can use the Previous

### S11.10b Windchill part structure comparison: Previous difference / Next difference; export to a file

- Used for: buttons on the result page; Excel export
- URL: https://support.ptc.com/help/windchill/r13.0.2.0/en/Windchill_Help_Center/prodstructure/PMStructureComparePartResults.html
- Saved copy: `windchill_PMStructureComparePartResults.txt` (fetched 2026-10-29)
- Quote (saved line 18):
> Previous difference Next difference
- Quote (saved line 29):
> Comparison results can be exported to a file on your desktop using the Export list to file action.

### S11.10c Oracle Fusion structure comparison: changed attributes are highlighted in yellow on the cell

- Used for: yellow cells
- URL: https://docs.oracle.com/en/cloud/saas/supply-chain-and-manufacturing/26d/faupd/compare-structures-new.html
- Saved copy: `oracle_fusion_compare_structures.txt` (fetched 2026-10-29)
- Quote (saved line 66):
> differences are highlighted in yellow on the cell. The attributes include

### S11.10d OpenBOM: the change report is an Excel file; change management is on the Company plan

- Used for: Excel export; README comparison
- URL: https://help.openbom.com/my-openbom/change-management-and-revision-control/
- Saved copy: `openbom_change_revision.txt` (fetched 2026-10-29)
- Quote (saved line 3):
> The output of the change report is an Excel file
- Quote (saved line 3):
> This capability is only available for Company subscriptions.

### S11.10e OpenBOM pricing page: Company plan price

- Used for: README comparison (the price is quoted from this page only)
- URL: https://www.openbom.com/pricing
- Saved copy: `openbom_pricing.txt` (fetched 2026-10-29)
- Quote (saved line 142):
> For growing companies with PDM and PLM needs
- Quote (saved line 143):
> $165
- Quote (saved line 144):
> $90
- Quote (saved line 129):
> billed monthly
- Quote (saved line 130):
> billed yearly

### S11.11 Alternative for the screen's base: Jakarta Servlet

- Used for: ADR 0003
- URL: https://jakarta.ee/specifications/servlet/
- Saved copy: `jakarta_servlet_spec_page.txt` (fetched 2026-10-29)
- Quote (saved line 214):
> Jakarta Servlet defines a server-side API for handling HTTP requests and responses.

### S11.11b Alternative for the screen's base: Spring Boot

- Used for: ADR 0003
- URL: https://docs.spring.io/spring-boot/reference/web/servlet.html
- Saved copy: `springboot_servlet_web.txt` (fetched 2026-10-29)
- Quote (saved line 1124):
> For servlet application, Spring Boot includes support for embedded Tomcat and Jetty servers.

### S11.12 Apache POI project page (what it is; the newest release)

- Used for: ADR 0004
- URL: https://poi.apache.org/
- Saved copy: `poi_apache_org_index.txt` (fetched 2026-10-29)
- Quote (saved line 4):
> Apache POI™ - the Java API for Microsoft Documents
- Quote (saved line 32):
> 22 October 2026 - POI 6.0.0 available
- Quote (saved line 39):
> POI requires Java 17 or newer since version 6.0.0.

### S11.13 OSV: advisory for log4j-api 2.24.3 (GHSA-qv9r-c865-cp47)

- Used for: pom.xml dependencyManagement; ADR 0004
- URL: https://api.osv.dev/v1/vulns/GHSA-qv9r-c865-cp47
- Saved copy: `osv_GHSA-qv9r-c865-cp47.txt` (fetched 2026-10-29)
- Quote (saved line 78):
> "fixed": "2.25.5"
- Quote (saved line 6):
> Users are advised to upgrade to Apache Log4j API 2.25.5 or 2.26.1
- Quote (saved line 5):
> Improper encoding of non-finite floating-point values during MapMessage JSON serialization

### S11.13b OSV query result, one request per library (poi-ooxml 5.5.1 and its 12 compile dependencies; log4j-api 2.24.3 for comparison)

- Used for: ADR 0004; README (dependency check)
- URL: https://api.osv.dev/v1/query (POST, one request per line below; ecosystem Maven)
- Saved copy: `osv_query_poi_dependencies.txt` (fetched 2026-10-29)
- Quote (saved line 3):
> org.apache.poi:poi-ooxml 5.5.1 -> 0 vulnerabilities []
- Quote (saved line 14):
> org.apache.logging.log4j:log4j-api 2.25.5 -> 0 vulnerabilities []
- Quote (saved line 16):
> org.apache.logging.log4j:log4j-api 2.24.3 -> 1 vulnerabilities [('GHSA-qv9r-c865-cp47', ['CVE-2026-49844'])]

### S11.13c OSV query result for the test libraries and the Maven plugins (24 libraries in the file, 0 findings)

- Used for: README (dependency check)
- URL: https://api.osv.dev/v1/query (POST, one request per line below; ecosystem Maven)
- Saved copy: `osv_query_pom_all.txt` (fetched 2026-10-29)
- Quote (saved line 16):
> org.junit.jupiter:junit-jupiter-api 5.10.2 -> 0 vulnerabilities []
- Quote (saved line 26):
> org.apache.maven.surefire:surefire-junit-platform 3.2.5 -> 0 vulnerabilities []
- Quote (saved line 27):
> TOTAL 24 libraries, 0 vulnerabilities
