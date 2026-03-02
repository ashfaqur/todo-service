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

- `description` and `dueAt` time are mandatory fields for creating a todo task.
- `dueAt` time must be greater than or equal to current time at creation. Past `dueAt` is rejected with `400`.
- Overdue transition (`NOT_DONE -> PAST_DUE`) for a todo task is performed automatically during API operations:
  - before list/read responses
  - before any mutation operations
- `PAST_DUE` items are immutable and attempt to update will return `409`.
- Reopening an already `DONE` todo item to `NOT_DONE` is rejected (returns `409`) when it is past the due date.
- No authentication/authorization is implemented.


## Run service with Docker (Recommended)

From repository root:

```bash
docker compose up --build
```

Service is exposed on `http://localhost:8080`.

## Build and run manually

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


