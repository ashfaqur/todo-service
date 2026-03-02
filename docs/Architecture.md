# Architecture

## 1. Overview
This service is a Spring Boot backend for managing todo items with due dates and controlled status transitions.
It exposes REST endpoints for create, read (single and list), update description, mark done, and mark not-done.

Key challenge constraints reflected in the implementation:
- No authentication/authorization layer.
- H2 in-memory database with Flyway migration for schema creation.
- Docker support (`service/Dockerfile`, root `docker-compose.yml`).
- Automated tests across unit, controller, repository, and integration scopes.

## 2. Package Structure and Separation of Concerns
Primary package: `com.demo.todo`

- `controller`
  - `TodoRestController` contains routing and delegation only.
  - OpenAPI endpoint annotations are intentionally moved out of controller code.
- `openapi`
  - `TodoApi` holds endpoint documentation annotations.
  - `OpenApiExamples` centralizes JSON examples used by docs.
- `service`
  - `TodoService` orchestrates API use cases and captures current time (`Instant.now(clock)`).
  - `DataService` is the transaction boundary and central place for persistence/state rules.
- `repository`
  - `TodoRepository` extends Spring Data JPA and adds custom SQL update queries for overdue sync.
- `model`
  - `Todo` JPA entity and `TodoStatus` enum.
- `dto`
  - Request/response records for API contracts.
- `exception`
  - Domain exceptions plus `GlobalExceptionHandler` for consistent HTTP error mapping.
- `config`
  - `TimeConfig` (`Clock` bean) and `OpenApiConfig` metadata.

Design intent: keep HTTP concerns, business orchestration, transactions, and persistence responsibilities clearly separated.

## 3. Domain Model
### Todo entity
`Todo` fields:
- `id`: auto-increment `Long` primary key.
- `description`: task text (`VARCHAR(1000)` in schema).
- `status`: `NOT_DONE`, `DONE`, `PAST_DUE`.
- `createdAt`: timestamp set during creation.
- `dueAt`: client-provided due timestamp.
- `doneAt`: nullable completion timestamp.

### TodoStatus
- `NOT_DONE`: active and mutable.
- `DONE`: completed state.
- `PAST_DUE`: overdue and immutable via REST operations.

### State machine rules
- Overdue transition:
  - `NOT_DONE -> PAST_DUE` happens during sync operations (not via background scheduler).
- Mark done:
  - `NOT_DONE -> DONE` with `doneAt = now`.
  - `DONE -> DONE` is idempotent.
- Mark not done:
  - `DONE -> NOT_DONE` with `doneAt = null` only if `dueAt >= now`.
  - `NOT_DONE -> NOT_DONE` is idempotent.
- Immutability:
  - Any write on `PAST_DUE` returns conflict.
  - Reopening overdue `DONE` items is forbidden (`OVERDUE_REOPEN_FORBIDDEN`).

## 4. Overdue Synchronization Strategy
The service uses **on-demand synchronization** rather than a scheduled job.

Sync timing:
- Before reads:
  - Single item (`GET /todos/{id}`): sync by id, then fetch.
  - List (`GET /todos`): bulk sync, then query list.
- Before writes:
  - Update/mark done/mark not-done all sync by id first.

List vs single sync:
- Bulk list sync updates all overdue `NOT_DONE` rows.
- Single sync targets one id.

Representative repository SQL:

```sql
UPDATE todos
SET status = 'PAST_DUE'
WHERE status = 'NOT_DONE'
  AND due_at < :now
```

Time source:
- Application time is captured via injected `Clock` and passed into queries.
- SQL does not rely on `CURRENT_TIMESTAMP`.

## 5. DTOs and Mapping
Request DTOs:
- `CreateTodoRequest` (`description`, `dueAt`)
- `UpdateDescriptionRequest` (`description`)

Response DTOs:
- `TodoResponse` (single item)
- `TodosListResponse` + `TodosListMeta` (list + metadata)
- `ErrorResponse` (`error`, `message`, `path`, `timestamp`)

Validation and mapping:
- Input validation is applied at DTO level (`@NotBlank`, `@NotNull`, `@Size(max=1000)`).
- Entity-to-response mapping is handled in `TodoService` (`toResponse` helper).
- `GlobalExceptionHandler` maps domain and validation exceptions into uniform `ErrorResponse` payloads.

## 6. Testing Approach
The test suite combines isolated unit tests with persistence and end-to-end integration tests.

- Unit tests
  - `TodoServiceTest`: orchestration behavior, time-driven validation, mapping/delegation.
  - `DataServiceTest`: transactional state rules, immutability, idempotency, conflict paths.
- Controller tests (`@WebMvcTest`)
  - `TodoRestControllerTest`: endpoint contract, validation errors, and exception-to-HTTP mapping with mocked service.
- Persistence/integration tests
  - `TodoRepositoryIntegrationTest` (`@DataJpaTest`): custom SQL updates and sorted queries.
  - `DataServiceIntegrationTest` (`@SpringBootTest`): state rules + read/write sync against real DB wiring.
  - `TodoApiIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`): real HTTP + DB behavior.

Why `Clock` injection matters:
- Tests can use fixed timestamps, making overdue and boundary behavior deterministic and repeatable.

## 7. Trade-offs and Future Improvements
- Add scheduler for background overdue transitions to reduce per-request sync overhead.
- Add pagination for large todo lists.
- Add authentication and authorization for multi-user scenarios.
- Consider UUID-based identifiers for distributed/multi-database deployments.
- Introduce API versioning and stronger backward-compatibility policies if the API evolves.
