### Overview
- **Purpose**: JAX-RS (Jersey) REST resources for `Events`, `Objects`, `Sets`, and `Functions` with Swagger annotations.

### Endpoints
- `EventsResource`:
  - GET `/events/{namespace}` with query: time window, metadata/dimensions filters, payload toggle, sort, limit.
  - GET `/events/{namespace}/metadata/{metadata}`
  - GET `/events/{namespace}/dimension/{dimension}`
  - PUT `/events/{namespace}` (create), POST `/events/{namespace}` (store batch), DELETE `/events/{namespace}` (drop), DELETE `/events/expire/{namespace}/{end}`
- `ObjectsResource`:
  - PUT `/objects/{namespace}` (create), DELETE `/objects/{namespace}` (drop)
  - PUT `/objects/{namespace}/{key}` (store bytes), GET `/objects/{namespace}/{key}` (base64 JSON), DELETE `/objects/{namespace}/{key}`
  - GET `/objects/keys/{namespace}` and `/objects/keys/{namespace}/{prefix}`; GET `/objects/size/{namespace}`
- `SetsResource`:
  - PUT `/sets/{namespace}` (create), DELETE `/sets/{namespace}` (drop)
  - PUT `/sets/{namespace}/{set}/{entry}/{weight}` (add)
  - GET `/sets/{namespace}` (list sets), GET `/sets/{namespace}/{set}` (entries with weights), GET `/sets/entries/{namespace}/{set}` (entry names)
  - GET `/sets/weight/{namespace}/{set}/{entry}`, GET `/sets/size/{namespace}/{set}`
  - GET `/sets/union/{namespace}`, GET `/sets/intersect/{namespace}`
  - DELETE `/sets/{namespace}/{set}` (delete between), DELETE `/sets/{namespace}/{set}/{entry}` (delete entry), DELETE `/sets/pop/{namespace}/{set}` (pop)

### Implementation details
- Parameter binding via `@BeanParam` beans that normalize bounds and parse query lists to Cantor query maps.
- JSON via Gson; payloads base64-encoded transparently.
- Errors: consistent 400/500 mapping; logs include parameters.

### Dependencies
- Spring Boot Jersey starter, Swagger (`swagger-jaxrs2` + `classgraph`), Gson, Logback.
