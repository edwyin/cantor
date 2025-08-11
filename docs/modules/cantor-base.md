### Overview
- **Purpose**: Defines the core storage abstractions and contracts for Cantor.
- **Key interfaces**: `Cantor`, `Objects`, `Sets`, `Events`, `Namespaceable`.
- **Consumers**: All implementation modules (e.g., `cantor-jdbc`, `cantor-h2`, `cantor-mysql`, `cantor-s3`, `cantor-grpc-*`) and wrappers (`cantor-misc`, `cantor-metrics`, `cantor-functions`).

### Public API and semantics
- **Cantor**: Facade to access the three services.
  - `objects()`, `sets()`, `events()`
- **Namespaceable**:
  - `create(namespace)`, `drop(namespace)` — create/drop a logical namespace in the underlying store.
- **Objects**: Key/value store of `String -> byte[]` per namespace.
  - `store(namespace, key, bytes)` and batch `store(namespace, Map<String, byte[]>)`
  - `get(namespace, key)` and batch `get(namespace, Collection<String>)`
  - `delete(namespace, key)` and batch `delete(namespace, Collection<String>)`
  - `keys(namespace, prefix?, start, count)` — pagination helpers
  - `size(namespace)`
- **Sets**: Sorted sets of string entries with `long` weights per namespace.
  - Mutations: `add(...)`, `delete(namespace, set, min, max)`, `delete(namespace, set, entry)`, `delete(namespace, set, entries)`
  - Reads: `entries(...)`, `get(...)`, `sets(namespace)`, `size(namespace, set)`, `weight(...)`, `first(...)`, `last(...)`
  - Algebra: `union(...)`, `intersect(...)`
  - Atomic ops: `inc(namespace, set, entry, count)`, `pop(...)`
- **Events**: Multi-dimensional time-series entries per namespace.
  - Event fields: `timestampMillis`, `metadata: Map<String,String>`, `dimensions: Map<String,Double>`, optional `payload: byte[]`.
  - Writes: `store(namespace, Event)` and batch `store(namespace, Collection<Event>)` (immutable entries)
  - Reads: `get(namespace, start, end, metadataQuery?, dimensionsQuery?, includePayloads=false, ascending=true, limit=0)`
  - Projections: `metadata(namespace, metadataKey, ...)`, `dimension(namespace, dimensionKey, ...)`
  - GC: `expire(namespace, endTimestampMillis)`

### Design notes
- **Namespaces**: All data is partitioned by a user-provided namespace string.
- **Convenience overloads**: Default methods provide ergonomic overloads delegating to canonical methods.
- **Queries**:
  - Metadata query supports exact/regex/not-equals (`=`, `~`, `!~`, `!=`).
  - Dimensions query supports numeric relational and range (`=`, `!=`, `>`, `>=`, `<`, `<=`, `a..b`).

### Integration points
- Storage backends implement these interfaces (e.g., JDBC-based, S3, gRPC).
- Higher-level wrappers (async, sharded, loggable, metrics) decorate these interfaces without changing contracts.

### Notable files
- `cantor-base/src/main/java/com/salesforce/cantor/Cantor.java`
- `cantor-base/src/main/java/com/salesforce/cantor/Objects.java`
- `cantor-base/src/main/java/com/salesforce/cantor/Sets.java`
- `cantor-base/src/main/java/com/salesforce/cantor/Events.java`
- `cantor-base/src/main/java/com/salesforce/cantor/Namespaceable.java`

### Dependency footprint
- Plain Java jar; no external runtime deps.

### Usage
Implementations return concrete instances behind the `Cantor` facade; clients call `cantor.events().store(...)`, `cantor.objects().get(...)`, etc.
