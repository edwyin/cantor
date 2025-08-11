### Overview
- **Purpose**: Shared utilities and providers used by multiple modules to reduce duplication and enforce invariants.
- **Key components**: `CommonPreconditions`, `CommonUtils`, `*Preconditions`, `*Provider` classes, test scaffolding.

### Responsibilities
- **Preconditions**: Argument validation helpers used widely (e.g., `checkNamespace`, `checkString`, events/objects/sets validation).
- **Providers**: Lightweight provider abstractions for `Events`, `Objects`, `Sets` to support composition or DI.
- **Factory**: `CantorFactory` here is a light utility; the runtime assembly factory lives in `cantor-server`.

### Notable files
- `cantor-common/src/main/java/com/salesforce/cantor/common/CommonPreconditions.java`
- `cantor-common/src/main/java/com/salesforce/cantor/common/CommonUtils.java`
- `cantor-common/src/main/java/com/salesforce/cantor/common/EventsPreconditions.java`
- `cantor-common/src/main/java/com/salesforce/cantor/common/ObjectsPreconditions.java`
- `cantor-common/src/main/java/com/salesforce/cantor/common/SetsPreconditions.java`
- `cantor-common/src/main/java/com/salesforce/cantor/common/*Provider.java`

### Dependencies
- Depends on `cantor-base` and `slf4j-api`; test scope uses `testng`.

### Usage
- All storage implementations and wrappers import these preconditions/utilities to ensure consistent validation and error messages.
