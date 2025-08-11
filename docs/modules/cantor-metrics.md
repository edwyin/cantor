### Overview
- **Purpose**: Dropwizard Metrics instrumentation wrappers for `Cantor` services.

### Architecture
- `MetricCollectingCantor` composes `MetricCollectingObjects`, `MetricCollectingSets`, `MetricCollectingEvents` around a delegate.
- Each wrapper records metrics (timers/counters) per method and propagates calls to the underlying implementation.

### Usage
- Construct with a `MetricRegistry` and a delegate `Cantor` (which can be wrapped/sharded/etc.).

### Notable files
- `cantor-metrics/src/main/java/com/salesforce/cantor/metrics/MetricCollectingCantor.java`
