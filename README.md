# Service Desk API

A REST API for an internal IT service desk: employees submit support tickets, and
IT staff triage, assign, and resolve them. This is a portfolio project and is under
active development.

## Tech stack

- Java 21, Spring Boot
- Spring Security (JWT-based authentication)
- Spring Data JPA, PostgreSQL, Flyway migrations
- Maven, Docker, Testcontainers

## Prerequisites

- JDK 21
- Docker (for PostgreSQL and for the integration tests)

## Configuration

Non-secret configuration lives in `src/main/resources/application.properties`.
Secrets and credentials are **not** committed — they are supplied as environment
variables at runtime.

### Required environment variables

| Variable                     | Description                                                        |
|------------------------------|--------------------------------------------------------------------|
| `SPRING_DATASOURCE_PASSWORD` | Password for the PostgreSQL user the app connects as.              |
| `JWT_SECRET`                 | Secret key used to sign JWTs. Must be at least 32 bytes (characters). |

The datasource URL (`jdbc:postgresql://localhost:5432/servicedesk`) and username
(`servicedesk`) are non-secret and live in `application.properties`.

## Running locally

1. Start a PostgreSQL 16 instance. For local development:

   ```bash
   docker run --name service-desk-db \
     -e POSTGRES_DB=servicedesk \
     -e POSTGRES_USER=servicedesk \
     -e POSTGRES_PASSWORD=devpassword \
     -p 5432:5432 -d postgres:16
   ```

2. Set the required environment variables (matching the password above):

   ```bash
   export SPRING_DATASOURCE_PASSWORD=devpassword
   export JWT_SECRET=<a-long-random-string-at-least-32-characters>
   ```

3. Run the application (Flyway applies the schema migrations on startup):

   ```bash
   ./mvnw spring-boot:run
   ```

   On Windows, use `mvnw.cmd` in place of `./mvnw`.

The API listens on `http://localhost:8080`.

## Authentication

- `POST /register` — create a user (open).
- `POST /login` — exchange valid credentials for a signed JWT.

## Observability

The API exposes operational endpoints and structured logs for monitoring in production.

### Actuator endpoints

Exposed under `/actuator`:

| Endpoint               | Access     | Purpose                                                           |
|------------------------|------------|-------------------------------------------------------------------|
| `/actuator/health`     | Public     | Liveness/readiness and dependency checks (database, disk).        |
| `/actuator/info`       | Admin only | Application/build info.                                           |
| `/actuator/metrics`    | Admin only | Metrics in JSON; drill into one via `/actuator/metrics/{name}`.   |
| `/actuator/prometheus` | Admin only | All metrics in Prometheus scrape format.                          |

Every endpoint except `/actuator/health` requires a JWT belonging to a user with the
`ADMIN` role. `/actuator/health` reports full component details.

### Business metrics

Alongside the standard JVM and HTTP metrics, the API publishes two domain counters:

| Metric            | Prometheus name         | Meaning                                       |
|-------------------|-------------------------|-----------------------------------------------|
| `tickets.created` | `tickets_created_total` | Total tickets created.                        |
| `tickets.resolved`| `tickets_resolved_total`| Total transitions into the `RESOLVED` status. |

### Structured logging and correlation IDs

Console logs are emitted as structured JSON (Elastic Common Schema) for ingestion by a
log aggregator. Each request is assigned a correlation ID, which is:

- read from the inbound `X-Correlation-Id` header when present, otherwise generated;
- attached to every log line produced while handling the request (the `correlationId` field);
- returned to the caller in the `X-Correlation-Id` response header.

This lets a single request be traced across all of its log lines, and lets a caller quote
the ID from a failed response when reporting a problem.

## Running the tests

```bash
./mvnw test       # unit and integration tests (Testcontainers starts PostgreSQL)
./mvnw verify     # full build including integration tests
```

Docker must be running, as the integration tests start a real PostgreSQL container.
