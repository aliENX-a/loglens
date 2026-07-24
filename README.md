# LogLens

**Log aggregation, search and alerting service**

LogLens is a small but complete backend service that collects logs from
different services, lets you search through them, shows simple metrics, and
raises alerts when something looks wrong. It is a demonstration implementation
of the kind of log/observability pipeline used in real systems: an app emits log
lines, they get ingested, stored, queried, and watched by alert rules.

It runs as a single Spring Boot application with no external services required
— the database is an embedded H2 file. That makes it easy to clone, build and
demo on a laptop.

---

## What it does

- **Ingest logs** over HTTP, one at a time or in batches.
- **Parse** both JSON log payloads and plain-text log lines (extracting level,
  service, timestamp and message).
- **Search** logs by service, level, keyword, trace id and time range.
- **Metrics** — total volume, error rate, level breakdown, per-service counts,
  and a time-bucketed histogram.
- **Alerting** with three rule types:
  - `THRESHOLD` — fire when more than N matching logs arrive in a window.
  - `ANOMALY` — fire when the current volume is statistically unusual compared
    to recent history (z-score).
  - `ABSENCE` — fire when a service goes silent (no logs in a window).
- **Auto-resolve** — when the condition clears, the alert is marked resolved.
- **Demo data** — seed realistic logs and inject live incidents for a demo.

---

## Tech stack

| Area        | Choice                                   |
|-------------|------------------------------------------|
| Language    | Java 17                                  |
| Framework   | Spring Boot 3.5.4 (Web, Data JPA)        |
| Database    | H2 (file-based, embedded)                |
| Build       | Maven                                    |
| API style   | REST (JSON) + a self-contained HTML UI  |
| Scheduling  | Spring `@Scheduled` for flush/scan/purge |

No message broker, no external DB, no cloud account needed.

---

## How to run

You need **Java 17+** and **Maven** on your machine.

```bash
# 1. build the jar
mvn clean package

# 2. run it
java -jar target/loglens-0.1.0.jar
```

The service starts on **http://localhost:8080**.

- REST API: `http://localhost:8080/api/v1/...`
- Dashboard UI: `http://localhost:8080/` (a single HTML page, no internet needed)
- H2 console: `http://localhost:8080/h2-console` (JDBC URL
  `jdbc:h2:file:./data/loglens`)

> The first run seeds 3 alert rules and ~2500 demo log events so the dashboard
> is not empty. Data is stored in `./data/` (gitignored).

### Run tests

```bash
mvn test
```

There are 18 unit tests covering the log parser, the anomaly detector and the
alert evaluation logic.

---

## Architecture

```
                   HTTP POST /api/v1/logs/ingest
                                 │
                                 ▼
                           ┌─────────────┐
                           │  LogParser  │  parses JSON + plain-text lines
                           └──────┬──────┘
                                  │ LogEvent
                                  ▼
                           ┌─────────────┐
                           │ IngestBuffer│  ArrayBlockingQueue,
                           │             │  batched + scheduled flush
                           └──────┬──────┘
                                  │ persist (batch)
                                  ▼
                      ┌──────────────────────┐
                      │    H2 store (file)   │
                      │  - LogEvent          │
                      │  - AlertRule         │
                      │  - Alert             │
                      └──────────┬───────────┘
                                 │
           ┌─────────────────────┼─────────────────────┐
           │                     │                     │
           ▼                     ▼                     ▼
    ┌────────────┐       ┌──────────────┐      ┌────────────────────┐
    │ QuerySvc   │       │ MetricsSvc   │      │  AlertEvalSvc      │
    └─────┬──────┘       └──────┬───────┘      │  (scheduled scan)  │
          │                     │              └─────────┬──────────┘
          ▼                     ▼                        │ writes Alert
 /api/v1/query       /api/v1/metrics/summary            ▼
                                                 ┌─────────────┐
                                                 │  H2 Alert   │
                                                 └──────┬──────┘
                                                        ▼
                                           /api/v1/alerts + dashboard
```

**Why a buffer?** Logs can arrive in bursts. Instead of writing each line to the
database one by one (slow, many small transactions), they are dropped into an
in-memory queue and flushed in batches on a timer. This keeps ingestion fast
under load and is the same pattern real log shippers use (buffer → batch →
flush).

---

## API reference

Base path: `/api/v1`

### Logs / ingestion

| Method | Path                  | Purpose                                       |
|--------|-----------------------|-----------------------------------------------|
| POST   | `/logs/ingest`        | Ingest one log event                          |
| POST   | `/logs/ingest/batch`  | Ingest a list of log events                   |
| POST   | `/logs/flush`         | Force the buffer to flush now                 |
| GET    | `/logs/stats`         | Buffer counters (accepted/written/dropped)    |

Ingest body (all fields optional except what you want to record):

```json
{
  "service": "payment-service",
  "level": "ERROR",
  "message": "db connection timeout",
  "traceId": "abc-123",
  "timestamp": "2026-07-10T14:03:00Z"
}
```

If you send a raw JSON object it is read from fields like `level`, `service`,
`msg`/`message`, `time`/`ts`/`timestamp`. Plain text lines are parsed for a
level word and a leading timestamp.

### Query

| Method | Path       | Purpose                                          |
|--------|------------|--------------------------------------------------|
| GET    | `/query`   | Search logs                                       |

Query params: `service`, `level`, `keyword`, `traceId`, `from`, `to`,
`minutes` (default 60), `page` (default 0), `size` (default 50).

### Metrics

| Method | Path                | Purpose                              |
|--------|---------------------|--------------------------------------|
| GET    | `/metrics/summary`  | Volume, error rate, by-level, by-service, histogram |

Params: `minutes` (default 60), `buckets` (default 30), `from`, `to`.

### Alert rules

| Method | Path             | Purpose                       |
|--------|------------------|-------------------------------|
| GET    | `/rules`         | List rules                    |
| GET    | `/rules/{id}`    | Get one rule                  |
| POST   | `/rules`         | Create rule                   |
| PUT    | `/rules/{id}`    | Update rule                   |
| DELETE | `/rules/{id}`    | Delete rule                   |
| POST   | `/rules/evaluate`| Force an alert scan now       |

### Alerts

| Method | Path              | Purpose                          |
|--------|-------------------|----------------------------------|
| GET    | `/alerts`         | List alerts (`status=FIRING`/`RESOLVED`/`ALL`) |
| GET    | `/alerts/count`   | `{ "firing": N, "resolved": M }` |
| POST   | `/alerts/{id}/resolve` | Manually resolve one         |

### Demo helpers

| Method | Path             | Purpose                                          |
|--------|------------------|--------------------------------------------------|
| POST   | `/demo/seed`     | Re-seed demo data (`?events=2500`)               |
| POST   | `/demo/incident` | Inject an error burst (`?service=&count=80`)     |
| POST   | `/demo/silence`  | Make a service go silent (`?service=&minutes=30`)|

These are handy for a live demo: seed data, then trigger an incident and watch
an alert fire on the dashboard.

---

## Alert rule types

### THRESHOLD
"More than `threshold` logs matching `level`/`keyword` for `service` in the last
`windowMinutes`." Example: more than 15 `ERROR` logs from `payment-service` in 5
minutes.

### ANOMALY
Compares the current window's volume against the last `baselineWindows`
windows. Computes a z-score (how many standard deviations away the current value
is from the baseline average). If `z > threshold`, it fires. This catches
"volume is weirdly high/low" without you hard-coding a number. Example:
`checkout-service` normally logs ~X/min; a sudden spike trips it.

### ABSENCE (heartbeat)
"Service `X` sent **zero** logs in the last `absenceMinutes`." This catches a
crash or network break where no error is even produced — the absence of signal
is the alarm. Marked `CRITICAL`.

### De-duplication and cooldown
To avoid alert spam, an alert is keyed by `ruleId | type | time-bucket`. A new
alert for the same rule in the same bucket is not created again, and a
`cooldownMinutes` window stops re-firing right after one already fired. When the
condition no longer holds, the open alert is auto-resolved.

---

## Project structure

```
src/main/java/com/jai/loglens/
  domain/         entities: LogEvent, Alert, AlertRule, enums
  dto/            request/response records
  ingestion/      LogParser, IngestBuffer, IngestionService/Controller
  query/          LogSpecification (JPA), QueryService/Controller
  metrics/        MetricsService/Controller
  alerting/       AlertRule, AlertEvaluationService, AnomalyDetector
  retention/      RetentionService (purge old logs)
  demo/           DemoDataService, DemoDataLoader, DemoController
  util/           TimeParser (timestamp normalization)
src/main/resources/
  application.yml
  static/index.html   (dashboard)
src/test/java/...      18 unit tests
```

---

## Notes / limitations

- The storage is a local H2 file — fine for a demo, not for production scale.
  Swapping in PostgreSQL is mostly a `datasource.url` + dependency change.
- Ingestion is in-memory buffered; a crash could lose a few seconds of buffered
  logs. Acceptable for the demo scope.
- Alert evaluation runs on a fixed schedule (15s), not as an instant stream
  processor. You can call `/rules/evaluate` to force it during a demo.
- Retention purges logs older than `loglens.retention.days` (default 7). The purge runs on an hourly schedule and is safe to run repeatedly.
