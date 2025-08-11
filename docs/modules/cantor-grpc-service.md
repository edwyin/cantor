### Overview
- **Purpose**: Server-side adapters exposing `Cantor` APIs via gRPC services.

### Responsibilities
- Map each RPC to the corresponding `Cantor` call, marshalling to/from proto messages.
- Honor gRPC cancellations and translate exceptions to proper gRPC errors via helper utils.
- Batch handling (e.g., `EventsService.storeBatch`).

### Notable files
- `EventsGrpcService`: `get/create/drop/store/storeBatch/metadata/dimension/expire` → `cantor.events()`.
- `ObjectsGrpcService`: `create/drop/keys/get/store/delete/size` → `cantor.objects()`.
- `SetsGrpcService`: full set surface (`get/union/intersect/pop/add/addBatch/delete*/keys/sets/size/weight/inc`) → `cantor.sets()`.

### Integration
- Embedded by `cantor-server` gRPC bootstrap; shares message size and thread pool tuning consistent with client.
