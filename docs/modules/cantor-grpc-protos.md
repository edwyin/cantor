### Overview
- **Purpose**: Protobuf definitions and generated stubs for gRPC services (`EventsService`, `ObjectsService`, `SetsService`).

### Services and messages
- `events.proto`:
  - Messages: `EventProto`, `GetRequest`, `GetResponse`, `MetadataRequest/Response`, `DimensionRequest/Response`, `ExpireRequest`, `StoreRequest(s)`, `VoidResponse`.
  - Service: `EventsService` with RPCs: `get`, `create`, `drop`, `store`, `storeBatch`, `metadata`, `dimension`, `expire`.
- `objects.proto`:
  - Messages: `Create/Drop/Keys/Get/Store/Delete/Size` requests + responses.
  - Service: `ObjectsService` with RPCs: `create`, `drop`, `keys`, `get`, `store`, `delete`, `size`.
- `sets.proto`:
  - Messages for set retrieval (`Get/Keys`), algebra (`Union/Intersect`), mutation (`Add`, `Delete*`, `Pop`, `Inc`), `Size`, `Weight`, etc.
  - Service: `SetsService` with corresponding RPCs.

### Build plugin
- Uses `protobuf-maven-plugin` with `protoc` and `protoc-gen-grpc-java` pinned via properties.

### Packages
- Java packages: `com.salesforce.cantor.grpc.events|objects|sets` (multiple files).

### Notes
- Large message allowance is configured on servers/clients (64MB) in other modules.
