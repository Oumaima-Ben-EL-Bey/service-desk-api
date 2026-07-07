# Service Desk API

[![CI](https://github.com/Oumaima-Ben-EL-Bey/service-desk-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Oumaima-Ben-EL-Bey/service-desk-api/actions/workflows/ci.yml)

A REST API for an internal IT service desk: employees submit support tickets, and
IT staff triage, assign, and resolve them. Built as a portfolio project to
demonstrate production-grade backend patterns — JWT authentication, role-based
authorization, database migrations, transactional workflow logic, and
observability.

## Live demo

| | |
|---|---|
| **API base URL** | https://service-desk-api.fly.dev |
| **Interactive docs (Swagger UI)** | https://service-desk-api.fly.dev/swagger-ui.html |
| **Health** | https://service-desk-api.fly.dev/actuator/health |

> The app auto-stops when idle to keep hosting costs near zero, so the **first
> request after a period of inactivity may take a few seconds** to wake it up.
> Subsequent requests are fast.

## Tech stack

- **Java 21**, **Spring Boot**
- **Spring Security** with JWT-based authentication and role-based access control
- **Spring Data JPA**, **PostgreSQL**, **Flyway** migrations
- **Spring Actuator** + structured JSON logging for observability
- **springdoc-openapi** for OpenAPI 3 / Swagger UI
- **Maven**, **Docker** / Docker Compose
- **JUnit 5**, **Mockito**, **Testcontainers**
- Deployed on **Fly.io** with a **Neon** serverless PostgreSQL database

## Architecture overview

The codebase is organised **feature-by-feature**, not layer-by-layer: each domain
module (`user`, `team`, `ticket`, `comment`) holds its own entity, repository,
service, controller, and DTOs, with cross-cutting code in `config` and `shared`.

- **Authentication** — registration and login issue a signed JWT (BCrypt-hashed
  passwords). A filter validates the token on each request and populates the
  security context.
- **Authorization** — three roles (`REQUESTER`, `AGENT`, `ADMIN`) enforced both
  *vertically* (which roles may call which endpoints) and *horizontally* (a
  requester sees only their own tickets; an agent sees their team's queue).
- **Schema** — all database schema is versioned as Flyway SQL migrations
  (`src/main/resources/db/migration`); Hibernate runs in `validate` mode and
  never modifies the schema.
- **Ticket lifecycle** — status transitions are validated, tickets are routed to
  teams by category, and the write paths run inside explicit transaction
  boundaries.
- **Observability** — Actuator health/metrics/Prometheus endpoints, custom
  business metrics (tickets created / resolved), and per-request correlation IDs
  propagated into structured JSON logs.

## API documentation

The API serves auto-generated OpenAPI 3 documentation:

- **Swagger UI** — `/swagger-ui.html` ([live](https://service-desk-api.fly.dev/swagger-ui.html))
- **OpenAPI spec (JSON)** — `/v3/api-docs`

Most endpoints require authentication. In Swagger UI, register and log in via the
open endpoints, copy the returned token, click **Authorize**, and paste it to
call the protected endpoints directly from the browser.

## Running locally

You need **JDK 21** and **Docker** (Docker is also required for the integration
tests, which start a real PostgreSQL container).

### Option A — Docker Compose (app + database together)

This runs the whole stack — the API and a PostgreSQL 16 database — with one
command.

1. Create a `.env` file in the project root (it is gitignored — never committed):

   ```
   SPRING_DATASOURCE_PASSWORD=devpassword
   JWT_SECRET=a-long-random-string-at-least-32-characters
   ```

2. Bring the stack up:

   ```bash
   docker compose up --build
   ```

The API is available at http://localhost:8080, and Swagger UI at
http://localhost:8080/swagger-ui.html. Stop it with `docker compose down` (add
`-v` to also discard the database volume).

### Option B — run the app directly

1. Start a PostgreSQL 16 database:

   ```bash
   docker compose up -d db
   ```

2. Set the required environment variables:

   ```bash
   export SPRING_DATASOURCE_PASSWORD=devpassword
   export JWT_SECRET=a-long-random-string-at-least-32-characters
   ```

3. Run the application (Flyway applies the schema migrations on startup):

   ```bash
   ./mvnw spring-boot:run
   ```

   On Windows, use `mvnw.cmd` in place of `./mvnw`.

## Configuration

Non-secret configuration lives in `src/main/resources/application.properties`.
Secrets are supplied as environment variables and are **never** committed.

### Required environment variables

| Variable                     | Description                                                          |
|------------------------------|----------------------------------------------------------------------|
| `SPRING_DATASOURCE_PASSWORD` | Password for the PostgreSQL user the app connects as.                |
| `JWT_SECRET`                 | Secret key used to sign JWTs. Must be at least 32 bytes (characters).|

The datasource URL and username are non-secret and live in
`application.properties`; in the deployed environment they are overridden by
`SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` to point at the managed
database.

## Authentication

- `POST /register` — create a user (open).
- `POST /login` — exchange valid credentials for a signed JWT.

Send the token as `Authorization: Bearer <token>` on all protected endpoints.

## Observability

The API exposes operational endpoints and structured logs for monitoring in
production.

### Actuator endpoints

| Endpoint               | Access     | Purpose                                                           |
|------------------------|------------|-------------------------------------------------------------------|
| `/actuator/health`     | Public     | Liveness/readiness and dependency checks (database, disk).        |
| `/actuator/info`       | Admin only | Application/build info.                                           |
| `/actuator/metrics`    | Admin only | Metrics in JSON; drill into one via `/actuator/metrics/{name}`.   |
| `/actuator/prometheus` | Admin only | All metrics in Prometheus scrape format.                          |

Every endpoint except `/actuator/health` requires a JWT belonging to a user with
the `ADMIN` role.

### Business metrics

| Metric            | Prometheus name         | Meaning                                       |
|-------------------|-------------------------|-----------------------------------------------|
| `tickets.created` | `tickets_created_total` | Total tickets created.                        |
| `tickets.resolved`| `tickets_resolved_total`| Total transitions into the `RESOLVED` status. |

### Structured logging and correlation IDs

Console logs are emitted as structured JSON (Elastic Common Schema). Each request
is assigned a correlation ID that is read from the inbound `X-Correlation-Id`
header (or generated), attached to every log line for that request, and returned
in the `X-Correlation-Id` response header — so a single request can be traced
across all of its log lines.

## Running the tests

```bash
./mvnw test       # unit and integration tests (Testcontainers starts PostgreSQL)
./mvnw verify     # full build including integration tests
```

Docker must be running, as the integration tests start a real PostgreSQL
container.

## Deployment

The app is packaged as a multi-stage Docker image and deployed on **Fly.io**,
backed by a **Neon** serverless PostgreSQL database. Pushes to `main` are
deployed automatically via GitHub Actions (`.github/workflows/fly-deploy.yml`).
Database credentials and the JWT secret are provided as Fly.io secrets and are
never stored in the repository.
