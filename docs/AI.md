# AI Usage

## Prompts

This spring boot project has been setup for development.
Given the database schema and the api design defined in the API_Design.md file, create a plan to implement only two rest endpoints.
1. Add item with POST /todos
2. Get single todo item with GET /todos/{id}
Make sure to use proper package architecture, for example:

todo
 ├── controller
 ├── service
 ├── repository
 ├── model
 ├── dto
 ├── exception

The plan should include a RestController class called TodoRestController for the endpoints.
A service class.
Entities.
Relevent DTO.
A controller advice class for handling exception.
Proper error responses as defined in the API design.

But for now skip the db bulk update logic to move todo items from NOT_DONE to PAST_DUE when it is past due to keep things simple.

For tests, focus on testing core functionality. Use mocked instances for unit tests. For controller use @WebMvcTest for testing, but mock the backend service class.

Polished version:

This Spring Boot project has already been set up for development.

Given:
- The database schema defined via Flyway (H2 in-memory)
- The API design defined in API_Design.md

Create a detailed implementation plan (not full code yet) for implementing ONLY the following two REST endpoints:

1. Add item → POST /todos
2. Get single todo → GET /todos/{id}

Follow this package structure:

todo
 ├── controller
 ├── service
 ├── repository
 ├── model
 ├── dto
 ├── exception

Requirements:

- Use a RestController class named `TodoRestController`.
- Use a service class (TodoService).
- Use a JPA entity for Todo.
- Use separate request/response DTOs.
- Use a @ControllerAdvice class for global exception handling.
- Use proper error responses as defined in API_Design.md.
- Use `Instant` for all timestamps (stored in UTC).
- The ID type must be `Long`.
- On creation:
  - description must be non-blank
  - dueAt is mandatory
  - initial status must always be NOT_DONE
  - doneAt must be null
- Do NOT implement bulk update logic for moving NOT_DONE → PAST_DUE yet.
- Verify Flyway migration already exists and ddl-auto=validate is configured.

Testing requirements:

- Write unit tests for the service layer using mocked repository.
- Write controller tests using @WebMvcTest.
- Mock the service layer in controller tests.
- Test:
  - 201 response for successful creation
  - 404 for get when id does not exist
  - 400 for invalid request input

Do NOT implement:
- Bulk update logic
- Scheduler
- Additional endpoints
- Pagination
- Authentication


In the plan 

1. Validate dueAt is not before Instant.now()

but for test determinism, consider:

TodoService will receive a Clock via constructor and use Instant.now(clock) to allow deterministic testing.

2. dueAt is not before Instant.now()

Service logic should:

Capture now

Validate dueAt >= now

Set createdAt = now

make sure this  aligns with rule in db

CHECK (due_at >= created_at) 



GlobalExceptionHandler also takes Clock via constructor

uses Instant.now(clock) for error timestamps


plan:

## Implementation Plan: `POST /todos` and `GET /todos/{id}`

### Summary
Implement only two endpoints using the required package structure and layering, with deterministic time handling via injected `Clock` in both `TodoService` and `GlobalExceptionHandler`. Enforce `dueAt >= now` in service, set `createdAt = now`, and keep behavior aligned with DB constraint `CHECK (due_at >= created_at)`. Use explicit `@Column(name=...)` for snake_case DB columns to guarantee JPA schema validation compatibility. Do not implement overdue transitions/bulk/scheduler/other endpoints.

### Confirmed Baseline
1. Flyway migration exists: [V1__create_todos_table.sql](/home/ash/sb/todo-service/service/src/main/resources/db/migration/V1__create_todos_table.sql), including `CHECK (due_at >= created_at)`.
2. JPA validation mode is configured: [application.properties](/home/ash/sb/todo-service/service/src/main/resources/application.properties) has `spring.jpa.hibernate.ddl-auto=validate`.
3. API reference file: [API_Design.md](/home/ash/sb/todo-service/docs/API_Design.md).

### Scope and Decisions Locked
1. Implement endpoints:
   1. `POST /todos`
   2. `GET /todos/{id}`
2. Do not perform `NOT_DONE -> PAST_DUE` transition yet, including single GET.
3. On `POST /todos`, if `dueAt` is before `now`, return `400 Bad Request`.
4. On successful creation:
   1. `status = NOT_DONE`
   2. `doneAt = null`
5. Deterministic time rule:
   1. `TodoService` receives `Clock` via constructor and uses `Instant.now(clock)`.
   2. `GlobalExceptionHandler` receives `Clock` via constructor and uses `Instant.now(clock)` for error timestamps.
6. Mapping rule:
   1. Use explicit `@Column(name = "...")` for snake_case columns (`created_at`, `due_at`, `done_at`) and other table columns for clarity/guaranteed schema validation behavior.

### Package and Class Plan
1. `com.demo.todo.model`
   1. `TodoStatus` enum: `NOT_DONE`, `DONE`, `PAST_DUE`.
   2. `Todo` JPA entity mapped to `todos`.
2. `com.demo.todo.repository`
   1. `TodoRepository extends JpaRepository<Todo, Long>`.
3. `com.demo.todo.dto`
   1. `CreateTodoRequest`
   2. `TodoResponse`
   3. `ErrorResponse`
4. `com.demo.todo.service`
   1. `TodoService` class, constructor-injected with `TodoRepository` and `Clock`.
   2. Methods:
      1. `TodoResponse createTodo(CreateTodoRequest request)`
      2. `TodoResponse getTodoById(Long id)`
5. `com.demo.todo.controller`
   1. `TodoRestController` with:
      1. `@PostMapping("/todos")`
      2. `@GetMapping("/todos/{id}")`
6. `com.demo.todo.exception`
   1. `TodoNotFoundException`
   2. `InvalidTodoInputException`
   3. `GlobalExceptionHandler` (`@ControllerAdvice`, constructor-injected `Clock`)
7. `com.demo.todo` (or `config` package)
   1. `@Bean Clock clock()` returning `Clock.systemUTC()` for production wiring.

### Data Model and Mapping Details
1. Entity `Todo`:
   1. `@Entity`
   2. `@Table(name = "todos")`
2. Field mapping with explicit columns:
   1. `id: Long`
      1. `@Id`
      2. `@GeneratedValue(strategy = IDENTITY)`
      3. `@Column(name = "id")`
   2. `description: String`
      1. `@Column(name = "description", nullable = false)`
   3. `status: TodoStatus`
      1. `@Enumerated(EnumType.STRING)`
      2. `@Column(name = "status", nullable = false)`
   4. `createdAt: Instant`
      1. `@Column(name = "created_at", nullable = false)`
   5. `dueAt: Instant`
      1. `@Column(name = "due_at", nullable = false)`
   6. `doneAt: Instant`
      1. `@Column(name = "done_at")`
3. ID type everywhere remains `Long`.
4. Use `Instant` end-to-end (entity + DTO + error timestamp).

### Service Logic (Creation) — Time and Constraint Alignment
1. In `createTodo`:
   1. Capture `Instant now = Instant.now(clock)`.
   2. Validate `request.dueAt >= now`:
      1. If `dueAt.isBefore(now)`, throw `InvalidTodoInputException` (`400`).
   3. Set `createdAt = now`.
   4. Set `status = NOT_DONE`.
   5. Set `doneAt = null`.
   6. Save and map to response.
2. Why this aligns with DB rule:
   1. Service enforces `dueAt >= createdAt` by comparing against the same captured `now` used for `createdAt`.
   2. DB `CHECK (due_at >= created_at)` remains the final safety net.
3. `GET /todos/{id}`:
   1. Load by ID or throw `TodoNotFoundException`.
   2. Return mapped response without overdue transition logic.

### API Contract Plan
1. `POST /todos`
   1. Request body:
      1. `description` required, non-blank (`@NotBlank`)
      2. `dueAt` required (`@NotNull`)
   2. Responses:
      1. `201 Created` + `Location: /todos/{id}` + `TodoResponse`
      2. `400 Bad Request` for validation failures and `dueAt < now`
2. `GET /todos/{id}`
   1. Responses:
      1. `200 OK` + `TodoResponse`
      2. `404 Not Found` when ID does not exist

### Error Handling Plan
1. Unified `ErrorResponse` fields:
   1. `error`
   2. `message`
   3. `path`
   4. `timestamp`
2. `GlobalExceptionHandler` behavior:
   1. Constructor-inject `Clock`.
   2. Build every error response with `timestamp = Instant.now(clock)`.
3. Exception mappings:
   1. `TodoNotFoundException` -> `404`, code `TODO_NOT_FOUND`
   2. `MethodArgumentNotValidException` -> `400`, code `INVALID_REQUEST`
   3. `HttpMessageNotReadableException` -> `400`, code `INVALID_REQUEST`
   4. `InvalidTodoInputException` -> `400`, code `INVALID_REQUEST`
4. Path source: `HttpServletRequest#getRequestURI()`.

### Implementation Sequence
1. Add model enum/entity with explicit `@Column(name=...)`.
2. Add repository.
3. Add DTOs.
4. Add exceptions.
5. Add `Clock` bean (`Clock.systemUTC()`).
6. Implement `TodoService` with constructor-injected `Clock`.
7. Implement `TodoRestController`.
8. Implement `GlobalExceptionHandler` with constructor-injected `Clock`.
9. Run tests and adjust serialization/validation/error formatting.

### Testing Plan

#### Service Unit Tests (mocked repository, deterministic clock)
1. Use fixed clock:
   1. `Clock fixedClock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC)`.
   2. Instantiate `TodoService(repo, fixedClock)`.
2. Cases:
   1. `createTodo_success_whenDueAtEqualsNow`:
      1. `dueAt == now` accepted.
      2. Assert `createdAt == now`, `status == NOT_DONE`, `doneAt == null`.
   2. `createTodo_success_whenDueAtAfterNow`.
   3. `createTodo_throwsInvalidTodoInput_whenDueAtBeforeNow`.
   4. `getTodoById_success`.
   5. `getTodoById_notFound_throwsTodoNotFoundException`.

#### Controller Tests (`@WebMvcTest`, mocked service)
1. Import/provide deterministic `Clock` bean for advice/controller test context (stable error timestamps).
2. `POST /todos` valid -> `201`.
3. `POST /todos` invalid payload (blank description/missing dueAt) -> `400`.
4. `GET /todos/{id}` missing -> mocked service throws `TodoNotFoundException` -> `404`.
5. Validate error response structure:
   1. `error`
   2. `message`
   3. `path`
   4. deterministic `timestamp`.

#### Integration Safety Check
1. `@SpringBootTest` context load must continue passing with `ddl-auto=validate`.
2. This implicitly verifies entity-column mapping aligns with Flyway schema (explicit snake_case columns mapped correctly).

### Acceptance Criteria
1. `POST /todos` enforces non-blank description and required dueAt.
2. `POST /todos` enforces `dueAt >= now` using `Clock`-based `now`.
3. `POST /todos` sets `createdAt = now`, `status = NOT_DONE`, `doneAt = null`.
4. `GlobalExceptionHandler` uses constructor-injected `Clock` and emits `Instant.now(clock)` in all error responses.
5. Service and error-timestamp behavior are deterministic in tests via fixed clock.
6. `GET /todos/{id}` returns `404` for missing IDs.
7. No overdue transition logic is present.
8. Entity uses explicit `@Column(name=...)` mappings for schema columns, including snake_case columns, and passes schema validation against Flyway DDL.

### Important Interface Additions
1. New controller: `TodoRestController`.
2. New service surface:
   1. `createTodo(CreateTodoRequest): TodoResponse`
   2. `getTodoById(Long): TodoResponse`
3. New persistence:
   1. `Todo` entity
   2. `TodoStatus` enum
   3. `TodoRepository`
4. New DTOs:
   1. `CreateTodoRequest`
   2. `TodoResponse`
   3. `ErrorResponse`
5. New exceptions/advice:
   1. `TodoNotFoundException`
   2. `InvalidTodoInputException`
   3. `GlobalExceptionHandler` (constructor-injected `Clock`)
6. New bean:
   1. `Clock` (`Clock.systemUTC()`)

### Assumptions and Defaults
1. Base package is `com.demo.todo` with requested subpackages.
2. Error code names are project-defined (`TODO_NOT_FOUND`, `INVALID_REQUEST`) while shape follows `API_Design.md`.
3. DB constraint `due_at >= created_at` is mirrored in service validation by shared `now` capture.
4. No additional endpoints, scheduler, pagination, auth, or bulk overdue updates in this phase.



Currently  the TodoService is mixing logic with acces to jpa repository.

Plan  a new service called DataService which will have the todoRepository and put data access code with transactional there only


Review the API_Design.md again.
Inlucde in the plan to add overdue (PAST_DUE) sync logic and also add a new REST endpoint for listing todos.


 Get all not-done (or all)

GET /todos?all=true|false

- all=false (default): return items with status NOT_DONE only
- all=true: return all items (NOT_DONE, DONE, PAST_DUE)

Past-due auto-update
- When listing, update overdue NOT_DONE items to become PAST_DUE before returning.
- Persist the changes

-> 200 OK

```json
{
  "items": [
    {
      "id": 123,
      "description": "Pay rent",
      "status": "NOT_DONE",
      "createdAt": "2026-02-28T09:30:00Z",
      "dueAt": "2026-03-01T10:00:00Z",
      "doneAt": null
    }
  ],
  "meta": {
    "count": 1,
    "all": false
  }
}
```

Past-due behavior requirements:
1) Bulk update for list:
   - Before returning list results, run a DB-level bulk update to persist overdue transitions:
     UPDATE todos
     SET status = 'PAST_DUE'
     WHERE status = 'NOT_DONE'
       AND due_at < :now;
   - Then query and return:
     - if all=false: SELECT items where status='NOT_DONE'
     - if all=true: SELECT all items
   - Use `Instant now = Instant.now(clock)` in service and pass `now` as parameter; do NOT use CURRENT_TIMESTAMP in SQL.

2) Single-item consistency:
   - For GET /todos/{id}, ensure the item is refreshed too:
     UPDATE todos
     SET status = 'PAST_DUE'
     WHERE id = :id
       AND status = 'NOT_DONE'
       AND due_at < :now;
     then fetch by id.
   - Only NOT_DONE can transition to PAST_DUE; DONE stays DONE even if overdue.

Implementation expectations:
- Add repository methods using @Modifying + @Query (native SQL is fine for UPDATE).
- Add service methods:
  - TodoResponse getTodoById(Long id) should do update-before-fetch.
  - A new method to list todos, e.g. TodosListResponse listTodos(boolean all).
- Add controller endpoint:
  - @GetMapping
    public ResponseEntity<TodosListResponse> listTodos(@RequestParam(defaultValue="false") boolean all)
- Ensure transaction boundaries:
  - update + fetch should be consistent within a service-layer transaction.
  - bulk update + list query should be consistent within a service-layer transaction.

Testing:
- Add/adjust tests to cover:
  - list endpoint triggers bulk overdue update (overdue NOT_DONE become PAST_DUE and are excluded from all=false)
  - list endpoint with all=true returns PAST_DUE items too
  - single GET updates overdue NOT_DONE to PAST_DUE
- Keep @WebMvcTest for controller tests (mock service).
- Use deterministic time with fixed Clock for service/integration tests.
- Prefer an integration-style test (@SpringBootTest or @DataJpaTest) for verifying the bulk update actually persists in H2.

Deliverable:
Write an updated step-by-step implementation plan (not full code) including:
- New DTO for list response (items + meta)
- Repository update/query methods
- Service flow for bulk + single refresh
- Controller changes
- Test plan updates
Do not add pagination, scheduler, authentication, or extra endpoints beyond GET /todos.


Add to the plan

Consider  Use @Modifying(clearAutomatically = true) to avoid stale persistence context issues.

sort order by createdAt for default ordering

findAll()
findByStatus()

## Updated Implementation Plan: Overdue Sync + `GET /todos` List Endpoint

### Summary
Extend the current implementation to add overdue synchronization (`NOT_DONE -> PAST_DUE`) on reads and introduce `GET /todos?all=true|false`. Keep `DataService` as the only repository-access and transaction boundary. Use deterministic time via injected `Clock` (`Instant.now(clock)`) and parameterized SQL (no `CURRENT_TIMESTAMP`). Add `@Modifying(clearAutomatically = true)` for update queries and enforce default list ordering by `createdAt`.

### Goals
1. Add DB-level overdue sync for:
   1. single read (`GET /todos/{id}`)
   2. list read (`GET /todos`)
2. Add list response DTO (`items + meta`).
3. Keep transactions for update+fetch and bulk-update+list consistent in `DataService`.
4. Apply default sort order by `createdAt` for both list modes (`all=true`, `all=false`).

### Out of Scope
1. Pagination
2. Scheduler
3. Authentication
4. Additional endpoints beyond `GET /todos` (plus existing `POST /todos`, `GET /todos/{id}`)
5. Any non-read bulk operations

---

### API/Contract Changes

#### 1) New endpoint
- `GET /todos?all=true|false`
- default `all=false`
- response:
  - `items`: array of `TodoResponse`
  - `meta`: `{ count, all }`

#### 2) Existing endpoint behavior change
- `GET /todos/{id}` now syncs overdue status before fetch:
  - overdue + `NOT_DONE` becomes `PAST_DUE`
  - `DONE` stays `DONE` even if overdue

---

### DTO Additions

1. `TodosListResponse`
   - fields:
     - `List<TodoResponse> items`
     - `TodosListMeta meta`
2. `TodosListMeta`
   - fields:
     - `int count`
     - `boolean all`

No changes to `TodoResponse` or error response schema.

---

### Repository Changes (`TodoRepository`)

Add methods:

1. Bulk overdue transition:
   - `@Modifying(clearAutomatically = true)`
   - `@Query` native:
     - `UPDATE todos SET status='PAST_DUE' WHERE status='NOT_DONE' AND due_at < :now`
   - `int markOverdueAsPastDue(Instant now)`

2. Single-item overdue transition:
   - `@Modifying(clearAutomatically = true)`
   - `@Query` native:
     - `UPDATE todos SET status='PAST_DUE' WHERE id=:id AND status='NOT_DONE' AND due_at < :now`
   - `int markOverdueAsPastDueById(Long id, Instant now)`

3. Sorted read methods (default order by createdAt):
   - `List<Todo> findAllByOrderByCreatedAtAsc()`
   - `List<Todo> findByStatusOrderByCreatedAtAsc(TodoStatus status)`

Notes:
- `findAll()` / `findByStatus()` are replaced in list flow by sorted variants.
- `clearAutomatically = true` prevents stale persistence-context reads after bulk updates.
- `DONE` never transitions in update predicates.

---

### Service Layer Design

## `TodoService` (business orchestration only)
Keeps mapping and high-level flow; delegates persistence/transactional consistency to `DataService`.

1. `createTodo(CreateTodoRequest request)` unchanged.
2. `getTodoById(Long id)`:
   - `now = Instant.now(clock)`
   - call `dataService.getByIdWithOverdueSync(id, now)`
   - throw `TodoNotFoundException` if empty
   - map to `TodoResponse`
3. new `listTodos(boolean all)`:
   - `now = Instant.now(clock)`
   - call `dataService.listWithOverdueSync(all, now)`
   - map entities to `TodoResponse`
   - return `TodosListResponse(items, meta(count, all))`

## `DataService` (repository + transactions)
Owns all repository calls and transaction boundaries.

1. existing `save(Todo)` remains transactional.
2. existing `findById(Long)` may remain for non-sync usage.
3. new `@Transactional` `Optional<Todo> getByIdWithOverdueSync(Long id, Instant now)`:
   - run `markOverdueAsPastDueById(id, now)`
   - then `findById(id)`
4. new `@Transactional` `List<Todo> listWithOverdueSync(boolean all, Instant now)`:
   - run `markOverdueAsPastDue(now)`
   - if `all=true`: `findAllByOrderByCreatedAtAsc()`
   - else: `findByStatusOrderByCreatedAtAsc(TodoStatus.NOT_DONE)`

---

### Controller Changes (`TodoRestController`)

1. Keep existing:
   - `POST /todos`
   - `GET /todos/{id}`
2. Add:
   - `@GetMapping`
   - `ResponseEntity<TodosListResponse> listTodos(@RequestParam(defaultValue = "false") boolean all)`
   - delegate to `todoService.listTodos(all)`
   - return `200 OK`

---

### Transaction and Consistency Rules

1. Single-item read consistency:
   - update-by-id + fetch in same `DataService` transaction.
2. List consistency:
   - bulk update + list query in same `DataService` transaction.
3. Persistence context freshness:
   - update queries use `@Modifying(clearAutomatically = true)` to avoid stale reads in same transaction.

---

### Step-by-Step Implementation Sequence

1. Add DTOs:
   - `TodosListResponse`, `TodosListMeta`
2. Extend `TodoRepository`:
   - two `@Modifying(clearAutomatically = true)` update queries
   - sorted list methods:
     - `findAllByOrderByCreatedAtAsc`
     - `findByStatusOrderByCreatedAtAsc`
3. Extend `DataService`:
   - `getByIdWithOverdueSync(id, now)`
   - `listWithOverdueSync(all, now)`
4. Update `TodoService`:
   - single GET uses sync-aware data call
   - add `listTodos(boolean all)`
5. Update `TodoRestController` with `GET /todos`.
6. Run and fix tests.

---

### Testing Plan

## 1) Repository/Integration-style tests (`@DataJpaTest` preferred)
Verify H2 persistence behavior:

1. `markOverdueAsPastDue(now)` updates only overdue `NOT_DONE`.
2. `markOverdueAsPastDueById(id, now)` updates only matching overdue `NOT_DONE`.
3. `DONE` never updated.
4. After update, sorted readers return ascending `createdAt` order.

## 2) `DataService` unit tests (Mockito)
Add/adjust:
1. `getByIdWithOverdueSync`:
   - verifies update call then find call.
2. `listWithOverdueSync(all=false)`:
   - verifies bulk update then `findByStatusOrderByCreatedAtAsc(NOT_DONE)`.
3. `listWithOverdueSync(all=true)`:
   - verifies bulk update then `findAllByOrderByCreatedAtAsc()`.

## 3) `TodoService` unit tests (Mockito + fixed Clock)
Add/adjust:
1. `getTodoById` delegates with captured `now` to sync-aware data method.
2. `listTodos(false)` builds `items + meta`, excludes past-due in fixture.
3. `listTodos(true)` builds `items + meta`, includes past-due in fixture.
4. keep existing create tests unchanged.

## 4) Controller tests (`@WebMvcTest`, mocked `TodoService`)
Add:
1. `GET /todos` default -> `meta.all=false`, expected `count`.
2. `GET /todos?all=true` -> `meta.all=true`, expected `count`.
3. Preserve existing tests for `POST /todos` and `GET /todos/{id}`.

Controller tests validate contract only, not DB update mechanics.

---

### Acceptance Criteria

1. `GET /todos/{id}` syncs overdue `NOT_DONE` to `PAST_DUE` before fetch.
2. `GET /todos` performs bulk overdue sync before list query.
3. `all=false` returns only `NOT_DONE`.
4. `all=true` returns all statuses.
5. Returned lists are sorted by `createdAt` ascending by default.
6. SQL updates use `now` parameter from `Instant.now(clock)` (no DB current-time function).
7. Repository update methods use `@Modifying(clearAutomatically = true)`.
8. `DataService` remains sole repository-access layer with transaction ownership.
9. Tests pass with deterministic time and integration verification in H2.

---

### Assumptions/Defaults

1. Default ordering is ascending by `createdAt` (`oldest first`).
2. `meta.count` equals `items.size()`.
3. Error response structure remains unchanged.
4. No pagination keys added to list response.
5. Existing behaviors for POST and validation remain unchanged.


Create a plan to add the following three rest endpoints

Since these are writes, they will trigger the db update to check for past due,
so we can reuse the logic we have already.


1. Update description

PATCH /todos/{id}/description

{
  "description": "Pay rent (landlord)"
}

-> 200 OK

```json
{
  "id": 123,
  "description": "Pay rent (landlord)",
  "status": "NOT_DONE",
  "createdAt": "2026-02-28T09:30:00Z",
  "dueAt": "2026-03-01T10:00:00Z",
  "doneAt": null
}
```
-> 404 Not Found → id not found
-> 400 Bad Request → blank/missing description
-> 409 Conflict → todo is PAST_DUE

2. Mark done

POST /todos/{id}/done

- If current status is NOT_DONE → set DONE, set doneAt = now
- If current status is DONE → idempotent: keep DONE, keep existing doneAt

-> 200 OK
```json
{
  "id": 123,
  "description": "Pay rent",
  "status": "DONE",
  "createdAt": "2026-02-28T09:30:00Z",
  "dueAt": "2026-03-01T10:00:00Z",
  "doneAt": "2026-02-28T09:45:12Z"
}
```
-> 404 Not Found
-> 409 Conflict → PAST_DUE

3. Mark not done

POST /todos/{id}/not-done

- If current status is DONE → set NOT_DONE, set doneAt = null
- If current status is NOT_DONE → idempotent:

-> 200 OK

```json
{
  "id": 123,
  "description": "Pay rent",
  "status": "NOT_DONE",
  "createdAt": "2026-02-28T09:30:00Z",
  "dueAt": "2026-03-01T10:00:00Z",
  "doneAt": null
}
```
-> 404 Not Found
-> 409 Conflict → PAST_DUE

## Plan: Add Write Endpoints for Description/Done/Not-Done with Past-Due Guard

### Summary
Implement three new write endpoints by reusing existing overdue-sync behavior (`update overdue before read`) so writes are blocked for `PAST_DUE` items. Keep `DataService` as repository + transaction boundary, keep deterministic time via injected `Clock`, and preserve existing API/error format.

### Endpoints to Add
1. `PATCH /todos/{id}/description`
2. `POST /todos/{id}/done`
3. `POST /todos/{id}/not-done`

### Behavior Rules (Decision Complete)
1. All three write endpoints must first run overdue sync for that ID:
   - `markOverdueAsPastDueById(id, now)` with `now = Instant.now(clock)`.
   - then fetch todo.
2. If todo not found -> `404`.
3. If todo status is `PAST_DUE` after sync -> `409 Conflict`.
4. `PATCH /description`:
   - request body must contain non-blank `description` (`400` on invalid).
   - update description (trim input), keep other fields unchanged.
5. `POST /done`:
   - `NOT_DONE` -> set `DONE`, set `doneAt = now`.
   - `DONE` -> idempotent, no change.
   - `PAST_DUE` -> `409`.
6. `POST /not-done`:
   - `DONE` -> set `NOT_DONE`, set `doneAt = null`.
   - `NOT_DONE` -> idempotent (ensure `doneAt` remains null).
   - `PAST_DUE` -> `409`.

---

### New/Updated Interfaces and Types

#### DTOs
1. Add `UpdateDescriptionRequest` in `dto`:
   - `@NotBlank String description`
2. Reuse `TodoResponse` for all success responses (200).

#### Exceptions
1. Add `PastDueImmutableException` in `exception`.
2. Map it in `GlobalExceptionHandler` to:
   - HTTP `409`
   - error code `"PAST_DUE_IMMUTABLE"`
   - message `"Past due items cannot be modified."`
   - keep current `{ error, message, path, timestamp }`.

#### Service APIs
1. `TodoService` add:
   - `TodoResponse updateDescription(Long id, UpdateDescriptionRequest request)`
   - `TodoResponse markDone(Long id)`
   - `TodoResponse markNotDone(Long id)`

2. `DataService` add transactional write methods:
   - `Todo updateDescription(Long id, String description, Instant now)`
   - `Todo markDone(Long id, Instant now)`
   - `Todo markNotDone(Long id, Instant now)`
   - These methods encapsulate:
     - overdue sync by ID
     - fetch by ID
     - state checks
     - mutation + save

---

### Repository Changes
Reuse current:
- `markOverdueAsPastDueById(Long id, Instant now)` (`@Modifying(clearAutomatically = true)`).

No new SQL update query is required for these three write endpoints beyond existing overdue-by-id update.

Optional helper read methods are not required; current `findById` + `save` is sufficient.

---

### Transaction and Layering Design

#### DataService (transaction boundary)
1. Each write method in `DataService` is `@Transactional`.
2. Sequence inside each write method:
   - `markOverdueAsPastDueById(id, now)`
   - `findById(id)` or throw `TodoNotFoundException`
   - if status `PAST_DUE` -> throw `PastDueImmutableException`
   - apply endpoint-specific mutation
   - `save(todo)` (or rely on dirty checking; choose explicit `save` for clarity/consistency)

#### TodoService (business orchestration)
1. Captures `now = Instant.now(clock)`.
2. Delegates to `DataService` write method.
3. Maps entity -> `TodoResponse`.
4. Keeps no repository access and no transaction annotations.

---

### Controller Changes (`TodoRestController`)
Add methods:
1. `@PatchMapping("/{id}/description")`
   - `ResponseEntity<TodoResponse> updateDescription(@PathVariable Long id, @Valid @RequestBody UpdateDescriptionRequest request)`
2. `@PostMapping("/{id}/done")`
   - `ResponseEntity<TodoResponse> markDone(@PathVariable Long id)`
3. `@PostMapping("/{id}/not-done")`
   - `ResponseEntity<TodoResponse> markNotDone(@PathVariable Long id)`

All return `200 OK` with `TodoResponse`.

---

### Detailed Implementation Steps
1. Add `UpdateDescriptionRequest` DTO.
2. Add `PastDueImmutableException`.
3. Extend `GlobalExceptionHandler` with `409` mapping for `PastDueImmutableException`.
4. Extend `DataService` with three new transactional write methods.
5. Extend `TodoService` with three public methods that pass `Instant.now(clock)`.
6. Extend `TodoRestController` with three endpoint handlers.
7. Keep existing create/get/list behavior intact.

---

### Test Plan

#### 1) Service Unit Tests (`TodoServiceTest`)
Mock `DataService`, fixed clock.
Add tests:
1. `updateDescription` delegates with fixed `now`, returns mapped response.
2. `markDone` delegates with fixed `now`, returns mapped response.
3. `markNotDone` delegates with fixed `now`, returns mapped response.

#### 2) DataService Unit Tests (`DataServiceTest`)
Mock `TodoRepository`.
Add tests for each method:
1. Overdue sync called first (`markOverdueAsPastDueById`).
2. Not found -> `TodoNotFoundException`.
3. `PAST_DUE` -> `PastDueImmutableException`.
4. Description updated for patch.
5. Done transition:
   - `NOT_DONE` -> `DONE`, `doneAt=now`.
   - `DONE` idempotent (doneAt unchanged).
6. Not-done transition:
   - `DONE` -> `NOT_DONE`, `doneAt=null`.
   - `NOT_DONE` idempotent (`doneAt` null).

#### 3) Controller Tests (`@WebMvcTest`)
Mock `TodoService`.
Add endpoint contract tests:
1. `PATCH /description`:
   - `200` success
   - `400` blank/missing description
   - `404` not found
   - `409` past-due conflict
2. `POST /done`:
   - `200` success
   - `404`
   - `409`
3. `POST /not-done`:
   - `200` success
   - `404`
   - `409`

#### 4) Integration Test (`@SpringBootTest` or `@DataJpaTest` + service)
Add focused integration scenarios to verify DB sync + write behavior persists in H2:
1. Overdue `NOT_DONE` then write call -> becomes `PAST_DUE` and write rejected with conflict path.
2. `DONE` item remains `DONE` when overdue and can be toggled via `/not-done` (not auto-converted to `PAST_DUE`).
3. Non-overdue `NOT_DONE` can be marked done and stores `doneAt`.

---

### Acceptance Criteria
1. Three endpoints are available with exact paths and semantics above.
2. All writes perform overdue-by-id sync before mutation.
3. `PAST_DUE` items are immutable via these write endpoints (`409`).
4. `DONE` is terminal regarding overdue sync (not auto-converted to `PAST_DUE`).
5. Validation errors on patch description return `400`.
6. Error payload format remains consistent with existing global handler.
7. Tests pass with deterministic time and coverage for transitions/idempotency/conflicts.

---

### Assumptions / Defaults
1. Conflict error code is `PAST_DUE_IMMUTABLE` (aligned with API design doc).
2. Description is trimmed before persistence.
3. Explicit `save` is used in `DataService` mutation methods for clarity.
4. No pagination/scheduler/auth/extra endpoints are introduced.


Create a detailed implementation plan (not full code) to adjust the existing Todo service write behavior with the following rule change:

## New Rule

If a todo is currently `DONE` and its `dueAt` is in the past (`dueAt < now`), the user must NOT be allowed to mark it as `NOT_DONE`.

In other words:
- DONE overdue → cannot be reopened.
- This must return HTTP 409 Conflict.
- Error code: `OVERDUE_REOPEN_FORBIDDEN`.

We are intentionally keeping the design where:
- `DONE` does NOT automatically transition to `PAST_DUE`.
- Only `NOT_DONE` items transition to `PAST_DUE` during overdue sync.
- `PAST_DUE` items remain immutable via REST API.

## Required Behavioral Adjustments

Modify only the `/not-done` flow:

### POST /todos/{id}/not-done

Existing behavior:
- sync overdue by ID
- fetch
- if PAST_DUE → 409
- if DONE → set NOT_DONE, doneAt=null
- if NOT_DONE → idempotent

New behavior:
- sync overdue by ID
- fetch
- if not found → 404
- if status == PAST_DUE → 409
- if status == DONE AND dueAt < now → 409 (OVERDUE_REOPEN_FORBIDDEN)
- if status == DONE AND dueAt >= now → allow transition to NOT_DONE
- if status == NOT_DONE → idempotent

All checks must use `Instant.now(clock)` captured once in TodoService and passed into DataService.

---

## Constraints

1. Do NOT change the overdue sync SQL logic.
2. Do NOT convert DONE overdue to PAST_DUE.
3. Keep DataService as the only transactional layer.
4. Keep controller thin.
5. Use existing error response structure:
   {
     error,
     message,
     path,
     timestamp
   }
6. Add a new exception class for this rule and map it in GlobalExceptionHandler to 409.
7. Do not introduce new endpoints or change existing endpoint paths.

---

## Deliverable

Write a structured step-by-step implementation plan including:

- Required changes in DataService (transactional flow)
- Required changes in TodoService (now handling)
- Exception class addition
- GlobalExceptionHandler update
- Controller (if any change required)
- Updated test plan:
  - unit tests for DataService
  - unit tests for TodoService
  - controller contract tests
  - integration test verifying overdue DONE cannot be reopened

Do not provide full code. Only a clear implementation plan.

---

## Additional Notes

Make sure the plan explicitly covers:

- exact order of checks inside DataService
- where `now` is captured
- how idempotency is preserved
- how this interacts with existing overdue sync logic
- ensuring deterministic time in tests via injected Clock

REview prompt

You are reviewing a Spring Boot coding challenge repository.

Your task is to:

1. Read the coding challenge objective Objective.md.
2. Analyze the current project implementation.
3. Compare the implementation against the objective.
4. Identify:
   - What is fully implemented
   - What partially meets requirements
   - What is missing
   - Any deviations or assumptions
   - Any design inconsistencies
5. Evaluate overall code quality, architecture, and test coverage.
6. Produce a structured review and write it to a file named `REVIEW.md`.


# Review Requirements

## Functional Requirements Review

For each functional requirement:

- State whether it is:
  - Fully implemented
  - Partially implemented
  - Missing
- Provide short reasoning.
- Reference specific classes/files where relevant.

Requirements include:
- Add item
- Update description
- Mark done
- Mark not done
- Get not-done items (with all=true option)
- Get single item
- Automatic overdue transition
- Forbid modifying past due items

---

## Non-Functional Requirements Review

Check:

- Dockerization
- H2 in-memory usage
- Automatic tests present
- No authentication implemented
- README completeness:
  - service description
  - assumptions
  - tech stack
  - build instructions
  - test instructions
  - run instructions

Mark each as:
- Implemented
-  Partial
-  Missing

---

## Design & Architecture Review

Evaluate:

- Layering (controller/service/repository separation)
- Transaction boundaries
- Time handling (Clock injection)
- Status state machine consistency
- Error handling consistency
- DTO usage
- Validation strategy
- DB constraints vs domain logic alignment
- Use of indices and query performance awareness
- Overdue sync implementation correctness

Highlight:
- Strengths
- Potential design improvements
- Over-engineering risks
- Missing edge-case handling

---

## Testing Review

Evaluate:

- Unit tests (service, data layer)
- Controller tests
- Integration tests
- Deterministic time usage
- Edge case coverage
- Idempotency tests
- Conflict scenarios
- Boundary condition tests

Indicate coverage gaps if any.

---

##  Remaining Work

Clearly list:

- What is still missing to fully satisfy the objective
- Any ambiguous interpretations that should be documented
- Any recommended improvements before submission

---

##  Overall Assessment

Provide:

- Overall compliance score (0–10)
- Code quality score (0–10)
- Readiness for submission (Ready / Minor fixes / Needs work)
- 3–5 bullet summary verdict

---

# Output Format

Generate a clean, structured Markdown document and write it to:

docs/REVIEW_3.md

Use clear sections:

- Executive Summary
- Functional Requirements
- Non-Functional Requirements
- Architecture & Design
- Testing
- Remaining Work
- Final Assessment

Do NOT modify any source code.
Only generate REVIEW.md.


Read all files in the repository.
Read the file `development.md`.

The goal of this document is to explain the development and thought process behind the project using clearly defined milestones.

Tasks:

1. Analyze:
   - The current `development.md`
   - The Git commit history
   - The files introduced or modified across commits

2. For each milestone listed in `development.md`:
   - Expand it with a short but meaningful description of:
     - What was implemented during that phase
     - What design decisions were made
     - What technical concerns were addressed
   - Keep the explanation high-level.
   - Do NOT add excessive detail or low-level code discussion.
   - Keep it concise and professional.
   - Preserve the milestone numbering and structure.

3. Check if there were some milestones missing and can be added to the list

4. Improve wording and grammar throughout the document.
   - Make it read like a structured development narrative.
   - Keep the tone professional and reviewer-facing.
   - Do not exaggerate or add features that were not implemented.

5. Do NOT:
   - Modify project code.
   - Invent development steps that did not occur.

Output:

- Rewrite and polish the full `development.md` file.
- Maintain Markdown formatting.
- Keep the document compact and readable.


Enhance this Spring Boot project by adding proper OpenAPI (Swagger) documentation.

Goal:
Add comprehensive OpenAPI documentation for all existing REST endpoints, including:
- API metadata (title, description, version)
- Endpoint summaries and descriptions
- Request/response schemas
- HTTP response codes
- Example request/response bodies
- Error response documentation
- Configuration for accessing Swagger UI

Tasks:

1. Add OpenAPI Dependency
   - Use springdoc-openapi (latest stable version compatible with Spring Boot 3).
   - Configure Swagger UI endpoint.
   - Ensure the app runs without breaking existing functionality.

2. Add Global API Metadata
   - Define:
     - title
     - description
     - version
     - contact (optional)
   - Use @OpenAPIDefinition or configuration bean.
   - Provide a short but professional API description.

3. Document All Endpoints
   For each controller method:
   - Add @Operation with:
     - summary
     - detailed description
   - Add @ApiResponses including:
     - 200 / 201 success
     - 400 validation errors
     - 404 not found
     - 409 conflict (where applicable)
   - Use @Parameter where helpful.
   - Document request body with @RequestBody and examples.
   - Document response schemas properly.

4. Add Schema Documentation
   - Annotate DTOs with @Schema:
     - field descriptions
     - examples
     - required fields
   - Ensure enums (TodoStatus) are properly documented.
   - Ensure error response model is documented clearly.

5. Add Usage Examples
   For each endpoint include:
   - Example request JSON
   - Example success response JSON
   - Example error response JSON (where applicable)

6. Configuration
   - Ensure Swagger UI is accessible at:
     /swagger-ui.html or /swagger-ui/index.html
   - Ensure OpenAPI JSON is accessible at:
     /v3/api-docs
   - Do not break existing tests.

7. Constraints
   - Do not modify business logic.
   - Do not refactor unrelated code.
   - Keep documentation concise and professional.
   - Avoid redundant descriptions.
   - Keep consistent terminology across endpoints.

Output:
- Modify controller and DTO files as needed.
- Add any required OpenAPI configuration classes.
- Ensure project compiles.
- Provide clean, maintainable documentation annotations.


Refactor the REST API documentation to reduce controller bloat by introducing an OpenAPI-annotated interface (Option D) and moving large example JSON strings into a dedicated constants class (Option B). Do not change runtime behavior.

## Current State
- `TodoRestController` contains both endpoint logic and extensive OpenAPI annotations + large inline JSON example strings.
- Endpoints and behavior must remain exactly the same.

## Goals
1. Make `TodoRestController` minimal and readable (routing + delegation only).
2. Move all OpenAPI annotations (@Operation, @ApiResponses, @Parameter, etc.) onto a new interface, e.g. `TodoApi`.
3. Move all JSON example String constants into a dedicated class, e.g. `com.demo.todo.openapi.OpenApiExamples`.
4. Keep Swagger/OpenAPI output equivalent in meaning:
   - Same title/tag grouping where applicable
   - Same response codes documented (200/201/400/404/409)
   - Same schemas (TodoResponse, TodosListResponse, ErrorResponse, etc.)
   - Same example payloads (just referenced from `OpenApiExamples`)

## Required Refactor Steps
1. Create package `com.demo.todo.openapi` (or similar).
2. Create `OpenApiExamples` class:
   - Move all `private static final String ... = """..."""` JSON examples from `TodoRestController` into this class.
   - Make them `public static final String ...`.
3. Create `TodoApi` interface (suggest package `com.demo.todo.controller.api` or `com.demo.todo.openapi`):
   - Copy OpenAPI annotations from the controller methods onto interface methods.
   - Interface methods must match existing endpoint signatures and return types.
   - Use `OpenApiExamples.*` for `@ExampleObject(value=...)`.
4. Update `TodoRestController`:
   - Keep `@RestController` and `@RequestMapping("/todos")`.
   - Implement `TodoApi`.
   - Keep Spring MVC annotations needed for runtime mapping (`@PostMapping`, `@GetMapping`, `@PatchMapping`, etc.) either:
     - on the interface (preferred if it works reliably in this project), OR
     - on the controller methods (acceptable if interface-only mapping causes issues).
   - Remove all OpenAPI annotations from the controller.
   - Keep the same method bodies and delegation to `TodoService`.
5. Ensure compilation and existing tests still pass.
6. Do not refactor business logic, services, repositories, or error handling.

## Notes / Constraints
- Do not change any endpoint paths, HTTP methods, request/response DTOs, or status codes.
- Do not change exception behavior or error response shape.
- Keep Tag grouping (e.g., `@Tag(name="Todos", ...)`) at the interface level if possible.
- Keep annotations concise; avoid duplication if you can reuse the same example constants.

## Deliverable
- Updated `TodoRestController` (clean + minimal)
- New `TodoApi` interface containing OpenAPI docs
- New `OpenApiExamples` class containing example JSON strings
- No changes to runtime behavior


Add proper JavaDoc documentation across the project, excluding the REST controller class which is already documented via the OpenAPI interface.

## Scope

Add JavaDoc to:

1. All classes except:
   - TodoRestController (since API documentation is provided via OpenAPI interface)

2. All public constructors

3. All public methods in:
   - Service layer
   - DataService layer
   - Repository interfaces (if meaningful)
   - DTOs (brief description only)
   - Exception classes
   - Configuration classes (e.g., Clock bean config)

## Requirements

### Class-Level JavaDoc
For each class:
- Provide a concise description of its responsibility.
- Mention its role in the architecture (e.g., “service layer”, “transaction boundary”, “DTO for API responses”).
- Avoid restating obvious code.
- Keep it professional and concise (2–6 lines).

### Constructor JavaDoc
For public constructors:
- Document purpose.
- Document parameters with `@param`.
- Explain injected dependencies where relevant (e.g., Clock, Repository).

### Method JavaDoc
For public methods:
- Describe:
  - What the method does
  - Important behavior (e.g., overdue sync, immutability rules)
  - Side effects (DB updates, state transitions)
- Include:
  - `@param`
  - `@return` (if applicable)
  - `@throws` for domain exceptions (e.g., TodoNotFoundException, PastDueImmutableException)

Keep method docs concise but meaningful.

### Constraints

- Do NOT modify business logic.
- Do NOT change method signatures.
- Do NOT add JavaDoc to private methods.
- Do NOT duplicate OpenAPI documentation in JavaDoc.
- Keep wording consistent and professional.
- Avoid excessive verbosity and keep text concise.

### Style Guidelines

- Use proper JavaDoc format (`/** ... */`).
- Use full sentences.
- Keep tone neutral and technical.
- Avoid redundant phrases like “This method will…”.

## Output

Update relevant files by adding appropriate JavaDoc comments only.
Do not refactor or restructure code.
Ensure the project compiles after changes.
