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
