### Overview
- **Purpose**: JDBC-based base implementations for storage engines (reused by H2/MySQL modules).

### Architecture
- `AbstractBaseCantorOnJdbc`:
  - Manages a special internal database (`cantor`) with a namespace lookup table.
  - Handles namespace create/drop: creates per-namespace databases and internal tables within a transaction.
  - Provides helpers for executing updates/batches with timeouts and retry on `SQLTransactionRollbackException`.
- `AbstractBaseEventsOnJdbc`:
  - Events are sharded into per-day chunk tables per namespace: `CANTOR-EVENTS-CHUNK-yyyy_MM_dd_<hashes>`.
  - A per-namespace lookup table (`CANTOR-EVENTS-CHUNKS-LOOKUP`) tracks chunk tables, columns, and start windows.
  - On write: builds batch inserts; creates chunk tables on-demand if missing.
  - On read: selects relevant chunks by time window and required metadata/dimension keys; builds SQL for metadata/dim queries; merges/sorts across chunks, enforces `limit`.
  - Supports `metadata(...)`, `dimension(...)`, and `expire(...)` via SQL primitives.
  - Regex handling is abstracted: subclasses provide `getRegexPattern/query/notQuery` to adapt to vendor.
- Similar abstract bases exist for `Objects` and `Sets` (see module sources).

### Performance/tuning
- Query timeout default: 30s.
- Uses cached thread pools to parallelize chunk reads; overall timeout guards applied.
- Indexing strategy: timestamp plus selective indices on metadata/dimension columns (vendor-specific limits apply).

### Notable files
- `cantor-jdbc/src/main/java/com/salesforce/cantor/jdbc/AbstractBaseCantorOnJdbc.java`
- `cantor-jdbc/src/main/java/com/salesforce/cantor/jdbc/AbstractBaseEventsOnJdbc.java`
- `cantor-jdbc/src/main/java/com/salesforce/cantor/jdbc/JdbcUtils.java`

### Dependencies
- `cantor-base`, `cantor-common`, HikariCP, Guava; test: Logback, TestNG.
