### Overview
- **Purpose**: Store and execute user-defined functions (code/templates) within Cantor namespaces.
- **Key classes**: `FunctionsOnCantor`, `Executor` + executors (`GroovyExecutor`, `FreemarkerExecutor`, `JavaExecutor`, `ScriptExecutor`, `ChainExecutor`).

### Architecture
- Functions are stored as objects in a derived namespace: `functions-<namespace>` via `Objects` API.
- Execution is delegated to an `Executor` discovered via `ServiceLoader`. Each executor declares supported file extensions.
- API surface (via `Functions`): `create/drop`, `store(name, body)`, `get(name)`, `list()`, `delete(name)`, `run(name, context, params)`.
- `Context` exposes a `Cantor` handle for functions to interact with data.

### Execution flow
1. `run(namespace, function, context, params)` reads the function body from `functions-<namespace>`.
2. Chooses an `Executor` based on the file extension (e.g., `.groovy`, `.ftl`).
3. Executor runs with provided `Context` and `params`.

### Notable files
- `cantor-functions/src/main/java/com/salesforce/cantor/functions/FunctionsOnCantor.java`
- `cantor-functions/src/main/java/com/salesforce/cantor/functions/Executor.java`
- `cantor-functions/src/main/resources/META-INF/services/com.salesforce.cantor.functions.Executor`

### Dependencies
- `cantor-base`, `cantor-common`, `groovy`, `freemarker`, `logback` (runtime).

### Operational notes
- Ensure executors are on the classpath and declared in `META-INF/services`.
- Function names must include an extension to select an executor.
