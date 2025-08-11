### Overview
- **Purpose**: MySQL-based storage implementation using JDBC bases.

### Implementation details
- `EventsOnMysql` extends `AbstractBaseEventsOnJdbc` and provides:
  - Vendor-specific DDL: `TEXT` for metadata values, `DOUBLE` for dimensions, `LONGBLOB` for payloads.
  - Indexing constraints: cap around 63 indexed columns; metadata indices use `(256)` prefixes.
  - Regex-like queries implemented via `LIKE` with `%` and `_`; original patterns are escaped and `*` mapped to `%`.
- Data source config via `MysqlDataSourceProperties` and provider.
- `CantorOnMysql` aggregates vendor implementations into a `Cantor`.

### Notable files
- `cantor-mysql/src/main/java/com/salesforce/cantor/mysql/EventsOnMysql.java`
- `cantor-mysql/src/main/java/com/salesforce/cantor/mysql/MysqlDataSourceProperties.java`

### Usage
- Recommended for production with appropriate indexing and partitioning; sharding and read/write splitting are composed in `cantor-server` via wrappers.
