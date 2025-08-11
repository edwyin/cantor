### Overview
- **Purpose**: S3-backed implementation for `Objects` and `Events` within a single bucket; `Sets` are not implemented.

### Implementation details
- `CantorOnS3` creates `ObjectsOnS3` and `EventsOnS3` using an `AmazonS3` client and bucket name.
- `sets()` throws `UnsupportedOperationException` — sets support is delegated to another backend when composed by `cantor-server`.
- Utilities: `S3Utils`, `StreamingObjects` for efficient IO.

### Notable files
- `cantor-s3/src/main/java/com/salesforce/cantor/s3/CantorOnS3.java`

### Usage
- Enabled via `cantor-server` when storage type is `s3`. `sets` must be provided by an additional configured backend.
