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

Service runs by default on: http://localhost:8080

Swagger UI: http://localhost:8080/swagger-ui/index.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

## API Endpoints

Base path: `/todos`

| Method | Endpoint | Params | Description |
| --- | --- | --- | --- |
| `POST` | `/todos` | body: `description`, `dueAt` | Create a new todo item. |
| `PATCH` | `/todos/{id}/description` | path: `id`; body: `description` | Update the description of an existing todo. |
| `POST` | `/todos/{id}/done` | path: `id` | Mark a todo as done (`NOT_DONE -> DONE`, idempotent for `DONE`). |
| `POST` | `/todos/{id}/not-done` | path: `id` | Mark a todo as not done, with overdue reopen restrictions. |
| `GET` | `/todos/{id}` | path: `id` | Get a single todo by ID. |
| `GET` | `/todos` | query: `all` (`false` default) | List todos (`all=false` for not-done only, `all=true` for all statuses). |


## API Assumptions and Behavior

Creation Rules
- `description` and `dueAt` are mandatory fields when creating a todo.
- `description` has a limit of 1000 characters.
- `dueAt` must be greater than or equal to the current time at creation (`dueAt >= now`).
- A `dueAt` in the past results in `400 Bad Request`.

Overdue Behavior
- The transition from `NOT_DONE` to `PAST_DUE` is performed automatically during API operations:
  - before returning single or list responses
  - before executing any mutation operations
- Only `NOT_DONE` items are automatically transitioned to `PAST_DUE`.
- `DONE` items are never auto-transitioned to `PAST_DUE`.
- An optional background scheduler can also run overdue synchronization in batches.
  - `todo.overdue-sync.scheduler.enabled` (default: `true`)
  - `todo.overdue-sync.scheduler.fixed-delay` (default: `PT1M`)

Immutability Rules
- `PAST_DUE` items are immutable. Any attempt to modify them returns `409 Conflict`.
- Reopening a `DONE` todo (`DONE → NOT_DONE`) is rejected with `409 Conflict` if the item is already past its `dueAt` timestamp.

Identifier Strategy
- Todo IDs use a database-generated auto-increment `BIGINT` primary key.
- This simplifies indexing and ensures efficient numeric lookups.
- The service is assumed to run against a single database instance.
- In distributed or horizontally scaled environments, a UUID-based strategy could be considered.

Timestamps
- All timestamps are represented in ISO-8601 format using UTC (e.g., `2026-03-01T10:00:00Z`).
- The service uses `Instant` internally for time calculations.
- The current time (`now`) is derived from an injected `Clock` to ensure deterministic and testable behavior.
- Overdue transitions are evaluated using application time (`dueAt < now`).
- Clients are expected to provide `dueAt` values in UTC.

Out of Scope
- Authentication and authorization are not implemented.
- Pagination for listing todos is not implemented.
  - The todo list is assumed to be small enough to be manageable in memory.
- Concurrency control is not implemented.
  - Concurrent modifications currently follow a last-write-wins model.
  - In a production environment, a @Version field (optimistic locking) could be introduced to detect and reject conflicting updates with a 409 Conflict response.
- In multi-instance deployments, each instance may run the scheduler unless coordinated externally.

## Run service with Docker

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

## Documentation:
- [Development process and milestones](docs/Development.md)
- [Architecture Overview](docs/Architecture.md)
- [API example usage](docs/Examples.md)
- [How AI was used in this project](docs/AI_Usage.md)
