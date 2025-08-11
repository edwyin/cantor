### Overview
- **Purpose**: Standalone server that boots gRPC services using a pluggable backend; config-driven.

### Boot process
- Main: `com.salesforce.cantor.server.Application` loads a Typesafe Config file path from argv.
- Sets JVM default timezone to UTC; installs SLF4J bridge for JUL.
- If `cantor.grpc.port` is configured, starts a gRPC `GrpcServer` (64MB max inbound, cached thread pool).

### Backend selection (`CantorFactory`)
- Reads `cantor.storage.type`:
  - `mysql`: One or more `CantorOnMysql`; compose `ReadWriteCantor` + `AsyncCantor`; multi-shard via env `MYSQL_SHARDS` or list config; `ShardedCantor` + `LoggableCantor`.
  - `h2`: Single or multiple `CantorOnH2`; optional sharding; `LoggableCantor`.
  - `s3`: `CantorOnS3` for objects/events plus delegated sets backend (`s3.sets.type`).
- Helpers: S3 client builder (region/endpoint/proxy), MySQL/H2 DS providers, executor creation.

### Configuration
- Helpers: `CantorEnvironment` (`getConfig`, `getConfigAsList`, `getConfigAsInteger` and env lookups).
- Constants define config keys and root prefix.

### Notable files
- `cantor-server/src/main/java/com/salesforce/cantor/server/Application.java`
- `cantor-server/src/main/java/com/salesforce/cantor/server/grpc/GrpcServer.java`
- `cantor-server/src/main/java/com/salesforce/cantor/server/utils/CantorFactory.java`

### Operations
- Graceful shutdown via JVM shutdown hook; worker threads named for traceability.
