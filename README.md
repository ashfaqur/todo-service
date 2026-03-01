# Todo Service

Spring Boot backend service for managing a simple to-do list with due dates and status transitions.

## Service Description

The service provides REST APIs to:
- add an item
- update item description
- mark item as done
- mark item as not done
- get not-done items (or all items with `all=true`)
- get a single item by id

Each todo contains:
- `description`
- `status`: `NOT_DONE`, `DONE`, `PAST_DUE`
- `createdAt`
- `dueAt`
- `doneAt`

## Current Assumptions and Behavior

- `dueAt` must be greater than or equal to current time at creation. Past `dueAt` is rejected with `400`.
- Overdue transition (`NOT_DONE -> PAST_DUE`) is performed automatically during API operations:
  - before list/read responses
  - before mutation operations
- `PAST_DUE` items are immutable through REST mutations (`description`, `done`, `not-done`) and return `409`.
- Reopening a `DONE` item to `NOT_DONE` is rejected when `dueAt < now` (returns `409`).
- No authentication/authorization is implemented.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Flyway migrations
- H2 in-memory database
- Springdoc OpenAPI UI
- Maven Wrapper (`./mvnw`)
- Docker / Docker Compose

## API Endpoints

Base path: `/todos`

- `POST /todos`
- `GET /todos/{id}`
- `GET /todos?all=false|true`
- `PATCH /todos/{id}/description`
- `POST /todos/{id}/done`
- `POST /todos/{id}/not-done`

## Build

```bash
cd service
./mvnw clean package
```

## Run Automatic Tests

```bash
cd service
./mvnw test
```

## Run Locally

```bash
cd service
./mvnw spring-boot:run
```

Service runs on `http://localhost:8080`.

Useful endpoints:
- Health: `http://localhost:8080/actuator/health`
- H2 Console: `http://localhost:8080/h2`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`

## Run with Docker

From repository root:

```bash
docker compose up --build
```

Service is exposed on `http://localhost:8080`.
