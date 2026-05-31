# Kicd

Kicd (pronounced "kicked") is a Kotlin library for working with ICD-10-CM classification data loaded from SQLite. It gives applications an in-memory ICD node tree, code range matching, ICD note reference parsing, and qualifier filtering for common "with ..." rules found in ICD documentation.

Kicd helps application code navigate and filter ICD taxonomy data. It does not determine diagnoses, clinical appropriateness, coding compliance, or billing correctness.

## Modules

### kicd-lib

The core library. It loads a generated ICD-10-CM SQLite database and exposes typed Kotlin objects for chapters, sections, and codes.

### kicd-db

Database artifact support. The module contains the SQLite schema used by Kicd databases, logging resources, and optional clinical-detail configuration resources.

## Requirements

- Java 17+
- Kotlin 2.x
- SQLite JDBC
- An ICD SQLite database using the schema in `kicd-db/src/main/resources/schema.sql`

## Installation

```kotlin
implementation("org.tekfive.kicd:kicd:1.0.0")
```

The published artifact coordinates are `org.tekfive.kicd:kicd`; the Kotlin package is `org.aideway.kicd`.

## Development

Kicd is built with Gradle. From a Gradle build that includes the `kicd-lib` and `kicd-db` modules, run the module tests:

```bash
./gradlew :kicd-lib:test
./gradlew :kicd-db:test
```

Build both modules:

```bash
./gradlew :kicd-lib:build :kicd-db:build
```

## Major Features

### ICD Database Loading

`DB` opens an ICD SQLite file, reads metadata from `icd_meta`, and loads all rows from `icd_nodes` into an in-memory tree. The loaded database exposes the ICD version, creation timestamp, root node, and optional clinical details.

```kotlin
import org.aideway.kicd.DB
import org.aideway.kicd.IcdNodeType

val db = DB("data/icd-10-2025.sqlite")

println(db.version)
println(db.createdAt)

val root = db.root
val code = root.findNode("B91", IcdNodeType.CODE)
val details = code?.let { db.getCodeDetails(it) }
```

The SQLite file must contain the `icd_meta`, `icd_nodes`, and optional `icd_node_clinical_details` tables described by the schema resource.

```bash
sqlite3 data/icd-10-2025.sqlite < kicd-db/src/main/resources/schema.sql
```

### ICD Node Tree

Kicd models the ICD hierarchy as a sealed node tree:

- `Root`
- `Chapter`
- `Section`
- `Code`

Every node has a database ID, name, description, parent, children, notes, include text, inclusion terms, and structured code-reference lists such as `excludes1`, `excludes2`, `codeFirst`, `codeAlso`, and `useAdditionalCode`.

```kotlin
import org.aideway.kicd.Code
import org.aideway.kicd.IcdNodeType

val root = db.root
val allNodes = root.flatten()
val allBillableCodes = root.billableCodes()

val d0511 = root.findNode("D05.11", IcdNodeType.CODE) as? Code
if (d0511 != null && d0511.billable) {
    println("${d0511.name}: ${d0511.description}")
}
```

Billable codes are leaf `Code` nodes. Non-billable code nodes have children that provide more specific billable classifications.

### Code Range Matching

`CodeRanges` represents one or more ICD ranges and can test individual codes or collect billable matches from a subtree.

```kotlin
import org.aideway.kicd.CodeRanges

val cancer = CodeRanges("C00", "C96")

check(cancer.isCodeBetween("C45.0"))
check(!cancer.isCodeBetween("H05.011"))

val matches = cancer.getBillableMatches(db.root)
println("Cancer billable codes: ${matches.size}")
```

Range parsing accepts single codes, ranges, comma-separated ranges, and common ICD note syntax such as trailing dashes.

```kotlin
val ranges = CodeRanges.parse("C00-C96, D00-D09")
    ?: error("expected valid ICD ranges")

val combined = CodeRanges.primaryCancerCodes + CodeRanges.preCancerousCodes
```

Internally, range matching distinguishes exact matches, candidate parent nodes, and non-candidates so searches can prune large parts of the ICD tree while still descending into partially matching branches.

### Code Qualifiers

ICD notes often contain constraints such as "with 7th character B", "with final characters .00 or .01", or "with fifth or sixth character 1-4 or 6". `CodeQualifier` parses supported qualifier text and filters candidate code lists.

```kotlin
import org.aideway.kicd.CodeQualifier

val qualifier = CodeQualifier.parse("with 7th character D")
    ?: error("unsupported qualifier")

val codes = listOf("S34.123D", "S34.123A", "G45.884")
val filtered = qualifier.filter(codes)

check(filtered == listOf("S34.123D"))
```

Qualifiers can also operate on `Code` objects while preserving the original code order.

```kotlin
val billableCodes = db.root.billableCodes()
val openFracture = CodeQualifier.parse("with open fracture 7th character")
val matchingCodes = openFracture?.filterCodes(billableCodes).orEmpty()
```

Supported qualifier families include:

- substring qualifiers, such as `with .14, .24, .94`
- final-character qualifiers, such as `with final characters -23`
- nth-character qualifiers, such as `with 7th character D`
- nth-character sequence qualifiers, such as `with fifth to sixth characters 51`
- open-fracture seventh-character qualifiers

### Code References

`CodeReference` parses human-readable ICD note references and extracts any embedded range and qualifier. This is useful for resolving `Excludes1`, `Excludes2`, `code first`, `code also`, and `use additional code` notes into concrete billable codes.

```kotlin
import org.aideway.kicd.CodeReference

val reference = CodeReference("open skull fracture (S02.- with 7th character B)")

println(reference.note)
println(reference.ranges?.values)
println(reference.qualifier != null)

val billableMatches = reference.findBillableCodes(db.root)
```

If a note contains a range, Kicd first finds billable codes inside that range. If the note also contains a qualifier, the qualifier is applied to the matched billable codes. If no range is present, the reference can be applied to a node's full billable-code subtree.

### ICD Note Fields

Each loaded node carries the major note fields from the ICD tabular structure. These fields are normalized from the SQLite representation into Kotlin lists.

```kotlin
val code = db.root.findNode("A00", IcdNodeType.CODE)

if (code != null) {
    println(code.notes)
    println(code.includes)
    println(code.inclusionTerms)
    println(code.excludes1)
    println(code.excludes2)
    println(code.codeFirst)
    println(code.codeAlso)
    println(code.useAdditionalCode)
}
```

Reference-bearing note fields are stored as `CodeReference` values, so downstream code can inspect the original note text, parsed ranges, parsed qualifiers, and resolved billable-code matches.

### Clinical Details

The optional `icd_node_clinical_details` table stores generated or curated clinical descriptions keyed by ICD node ID. `DB.getCodeDetails` returns the text when a row is available.

```kotlin
val code = db.root.findNode("A00.0", IcdNodeType.CODE)
val clinicalDetails = code?.let { db.getCodeDetails(it) }

if (clinicalDetails != null) {
    println(clinicalDetails)
}
```

Clinical details are auxiliary descriptive content. Applications should treat them as display or search support, not as authoritative coding guidance.

### SQLite Schema

Kicd databases use a small schema:

- `icd_meta`: version, title, and creation timestamp
- `icd_nodes`: hierarchical ICD node records and note fields
- `icd_node_clinical_details`: optional extended descriptions

The `icd_nodes` table stores parent-child relationships with `parent_id`, node type with `type_id`, and list fields using Kicd's unit-separator encoding. The library decodes those list fields when constructing nodes.

```sql
SELECT id, name, description, parent_id, type_id
FROM icd_nodes
ORDER BY type_id ASC, id ASC;
```

The loader expects parent rows to appear before their children in `type_id, id` order. Generated database files should preserve that ordering relationship.
