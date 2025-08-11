### Overview
- **Purpose**: Composable wrappers that add operational features without changing the `Cantor` API.

### Wrappers
- **Async** (`AsyncCantor`, `AsyncObjects`, `AsyncSets`, `AsyncEvents`): executes calls on an `ExecutorService`.
- **Loggable** (`LoggableCantor`, ...): logs method, parameters, and duration.
- **Read/Write Split** (`ReadWriteCantor`, ...): writes to one delegate and reads to another.
- **Sharded** (`ShardedCantor`, ...): consistent-hashes namespaces across multiple delegates.
- **Archivable** (`misc/archivable`): helpers to archive/migrate namespaces across backends.

### Notable files
- `cantor-misc/src/main/java/com/salesforce/cantor/misc/async/AsyncCantor.java`
- `cantor-misc/src/main/java/com/salesforce/cantor/misc/loggable/LoggableCantor.java`
- `cantor-misc/src/main/java/com/salesforce/cantor/misc/rw/ReadWriteCantor.java`
- `cantor-misc/src/main/java/com/salesforce/cantor/misc/sharded/ShardedCantor.java`

### Composition examples
- Wrap a gRPC client with logging and metrics; shard across multiple MySQL delegates; split R/W and add async for writes.
