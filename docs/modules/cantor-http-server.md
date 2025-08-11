### Overview
- **Purpose**: Embeds Jersey into Jetty, wires REST resources, and serves static Swagger UI.

### Boot process
- Main: `com.salesforce.cantor.http.jersey.Application` starts `EmbeddedHttpServer` at default port 8083 with base path `/api/*`.
- `EmbeddedHttpServer`:
  - Registers resources (`EventsResource`, `ObjectsResource`, `SetsResource`, `FunctionsResource`) with DI bindings.
  - Serves static assets from `cantor-http-server/src/main/resources/static`.
  - Chooses a `Cantor` instance (default: gRPC client to `localhost:7443`, wrapped with `LoggableCantor`). Helpers included for H2/MySQL alternatives.

### Dependencies
- `cantor-http-service`, `cantor-h2`, `cantor-mysql`, `cantor-grpc-client`, `cantor-misc`, Jersey (Jetty), Freemarker, Groovy.
