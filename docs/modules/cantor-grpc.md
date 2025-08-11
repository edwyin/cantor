### Overview
- **Purpose**: High-level view of gRPC components across modules.

### Components
- Protos: `cantor-grpc-protos`
- Client: `cantor-grpc-client` (`CantorOnGrpc`, `*OnGrpc`)
- Server adapters: `cantor-grpc-service` (`*GrpcService`)
- Server bootstrap: `cantor-server` (`GrpcServer`)

### Wireup
- Client connects to server target (`host:port`) with plaintext and 60s deadlines.
- Both sides set `maxInboundMessageSize` to 64MB.
- Cancellation and error propagation handled by service utils.
