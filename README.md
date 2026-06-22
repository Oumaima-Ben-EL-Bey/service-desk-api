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

## Running the tests

```bash
./mvnw test       # unit and integration tests (Testcontainers starts PostgreSQL)
./mvnw verify     # full build including integration tests
```

Docker must be running, as the integration tests start a real PostgreSQL container.
