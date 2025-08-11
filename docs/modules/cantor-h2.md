### Overview
- **Purpose**: H2-based storage implementation using JDBC bases.

### Implementation details
- `EventsOnH2` extends `AbstractBaseEventsOnJdbc` and provides:
  - H2-specific DDL for chunk and lookup tables.
  - Regex support via `REGEXP_LIKE(column, ?)`, with `*` translated to `.*` and anchored when prefix/suffix not wildcarded.
  - Database-level operations via `H2Utils` (create/drop db SQL).
- `CantorOnH2` aggregates vendor implementations into a `Cantor` (objects, sets, events).
- `H2DataSourceProvider`/`H2DataSourceProperties` configure file/in-memory, compression, pool sizing, timeouts.

### Notable files
- `cantor-h2/src/main/java/com/salesforce/cantor/h2/EventsOnH2.java`
- `cantor-h2/src/main/java/com/salesforce/cantor/h2/H2DataSourceProperties.java`
- `cantor-h2/src/main/java/com/salesforce/cantor/h2/CantorOnH2.java`

### Usage
- Suitable for local/dev or lightweight deployments. File-backed or in-memory modes supported.
