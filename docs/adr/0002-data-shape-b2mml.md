# ADR 0002: Use B2MML as the data shape

Status: accepted

Source entries (S...), author's decisions (D...) and searches without result (N...) refer to [../sources.md](../sources.md).

## Context

The tool compares two revisions of a manufacturing process plan (steps) and the material bill of each step. It needs a public, documented data shape that can be read and written without paying, so that sample data can be validated against an official schema, and so that the tool is not tied to one vendor's export.

Facts:

- B2MML is "an XML implementation of the ANSI/ISA-95" (S1.1). The current schema is version 0701 (2023) with the namespace `http://www.mesa.org/xml/B2MML` (S1.2).
- It has the objects needed: `OperationsDefinition` with `OperationsSegment` for the steps, `OperationsMaterialBill` with `OperationsMaterialBillItem` for the bill rows, linked by `OperationsMaterialBillID` and `MaterialSpecificationID` (S1.3, S1.4).
- Use is free for any purpose if MESA is acknowledged with a fixed sentence (S1.8).
- The text of the standard behind it is sold (S1.14); the XSD files and their documentation are free.
- There is no property element on a segment and no `MaterialDefinitionID` in a bill row (sources.md section 1 and N2, N3). The bundled example file carries a position number as a property named `Indice` (S1.6).
- No public sample has two revisions of one plan (N1).

## Decision

We will read and write the **B2MML** `OperationsDefinitionInformation` document: one `OperationsDefinition` with its `OperationsSegment` elements and, beside it, one `OperationsMaterialBill` per step. The official schema files of commit 65cc66534343c86a1ce953561e121f906a0a0cbe are bundled unchanged, and the sample data is validated against them in the tests.

Details that the schema leaves open (D1 to D5):

- The part number of a bill row is written in `MaterialSpecificationID` (D1).
- The position of a step is `ParameterSpecification` with `ID` `Indice` (D3).
- A step points to its bill with `OperationsMaterialBillID` (D2).

The acknowledgement sentence "The Business To Manufacturing Markup Language (B2MML) is used courtesy of MESA International." is in README and NOTICE, and the sample files carry it too.

## Alternatives considered

| candidate | evidence | why not |
|---|---|---|
| **STEP (ISO 10303), for example AP 240 process plans or AP 242** | "ISO10303 is not free"; AP 240 is "Process plans for machined products" (S1.10) | Paid text; AP 240 is about machined parts, not assembly-line plans. AP 242 ("Managed model based 3D engineering", S1.10) is a product-model standard; it was not read further. |
| **PLCS (STEP AP 239)** | AP 239 is "Product life cycle support" (S1.10); the OASIS technical committee was closed on 17 April 2017 (S1.11) | No maintained public body behind it. |
| **IPC-2581** | "a generic standard for printed circuit board" (S1.12) | Circuit boards only. |
| **CycloneDX (MBOM, formulation)** | has a field that "Describes how a component or service was manufactured or deployed." (S1.13) | Its reference key is only unique inside one document (S1.13), so it is not a key that holds across two revisions. |
| **B2MML JSON form** | the README says "There has been no checking of this file." (S1.15) | Unchecked. |
| **B2MML XML** | S1.1 to S1.8 | **Chosen.** |

## Consequences

- The sample data had to be invented and is marked fictional in each file (N1, D15).
- Because the bill row has no `MaterialDefinitionID`, the part number sits in `MaterialSpecificationID`. A real export may put it elsewhere (for example in a separate `OpMaterialSpecification` with a `MaterialDefinitionID`); this version does not follow that indirection (D1, D5).
- The position number of a step is a convention taken from the bundled example (D3). Files that order steps only by dependencies (`SegmentDependency`) are read in document order, and the dependencies are not compared (D5).
- 39 schema files are carried in the repository (about 1.2 MB). They are needed because the schema's `include` and `import` chain pulls in all of them.
- Schema validation uses the JDK only (`javax.xml.validation`), with external access switched off; a resolver that allows only the bundled file names serves the includes (SecureXml).
