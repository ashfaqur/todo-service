# Todo Service

Spring Boot backend service for managing a simple to-do list with due dates and status transitions.

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

## Service Description

The service provides REST APIs to:
- add an item
- update item description
- mark item as done
- mark item as not done
- get not-done items (or all items)
- get a single item by id

Each todo contains:
- `description`
- `status`: `NOT_DONE`, `DONE`, `PAST_DUE`
- `createdAt`
- `dueAt`
- `doneAt`

## API Endpoints

Base path: `/todos`

- `POST /todos`
- `PATCH /todos/{id}/description`
- `POST /todos/{id}/done`
- `POST /todos/{id}/not-done`
- `GET /todos/{id}`
- `GET /todos?all=false|true`


## API Assumptions and Behavior

Creation Rules
- `description` and `dueAt` are mandatory fields when creating a todo.
- `dueAt` must be greater than or equal to the current time at creation (`dueAt >= now`).
- A `dueAt` in the past results in `400 Bad Request`.

Overdue Behavior
- The transition from `NOT_DONE` to `PAST_DUE` is performed automatically during API operations:
  - before returning single or list responses
  - before executing any mutation operations
- Only `NOT_DONE` items are automatically transitioned to `PAST_DUE`.
- `DONE` items are never auto-transitioned to `PAST_DUE`.

Immutability Rules
- `PAST_DUE` items are immutable. Any attempt to modify them returns `409 Conflict`.
- Reopening a `DONE` todo (`DONE → NOT_DONE`) is rejected with `409 Conflict` if the item is already past its `dueAt` timestamp.

Out of Scope
- Authentication and authorization are not implemented.
- A scheduler for background state transitions is not implemented.
  - In a production system, a scheduled job could batch-update overdue items to reduce per-request synchronization overhead.
- Pagination for listing todos is not implemented.
  - The todo list is assumed to be small enough to be manageable in memory.

## Run service with Docker (Recommended)

From repository root:

```bash
docker compose up --build
```

Service is exposed on `http://localhost:8080`.

## Build and run manually

Requirements:
- Java 21
- Maven 3

Navigate to the service folder

```bash
cd service
```

Build the project

```bash
./mvnw clean package
```

Run Automatic Tests

```bash
./mvnw test
```

Run the service

```bash
./mvnw spring-boot:run
```

Service runs on `http://localhost:8080`.

## Useful endpoints:
- Health: `http://localhost:8080/actuator/health`
- H2 Console: `http://localhost:8080/h2`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`


