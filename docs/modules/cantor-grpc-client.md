### Overview
- **Purpose**: Client-side implementations of `Objects`, `Sets`, `Events` over gRPC; assembled into a `Cantor` via `CantorOnGrpc`.

### Architecture
- `AbstractBaseGrpcClient` manages:
  - `ManagedChannel` lifecycle, periodic refresh (every 10 minutes), and shutdown of old channels.
  - Stub creation with 60s deadlines and cached-thread-pool executor.
  - Exception translation from `StatusRuntimeException` to `IOException`.
- Concrete clients: `ObjectsOnGrpc`, `SetsOnGrpc`, `EventsOnGrpc` build type-safe calls to the proto services.
- `CantorOnGrpc(target)` wires the three clients for easy consumption.

### Configuration
- `target` is `host:port` (plaintext by default). Ensure server `maxInboundMessageSize` matches clients (64MB).

### Notable files
- `cantor-grpc-client/src/main/java/com/salesforce/cantor/grpc/AbstractBaseGrpcClient.java`
- `cantor-grpc-client/src/main/java/com/salesforce/cantor/grpc/CantorOnGrpc.java`
- `cantor-grpc-client/src/main/java/com/salesforce/cantor/grpc/*OnGrpc.java`
