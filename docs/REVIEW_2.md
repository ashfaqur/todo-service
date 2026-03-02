# Todo Service Challenge Review

## 1. Executive summary (pass/partial/fail + top 5 findings)

**Overall verdict: Partial**

Top 5 findings:
1. **Status value mismatch vs objective wording**: API/domain use `NOT_DONE`, `DONE`, `PAST_DUE` enums, while the objective specifies status values as `"not done"`, `"done"`, `"past due"` (`service/src/main/java/com/demo/todo/model/TodoStatus.java`, `docs/Objective.md`).
2. **Past-due transition is request-driven, not autonomous**: overdue sync runs before reads/mutations, but there is no scheduler/background transition; stale `NOT_DONE` rows remain until API access (`service/src/main/java/com/demo/todo/service/DataService.java`, `README.md` “Out of Scope”).
3. **Core functional requirements are largely implemented correctly**: create/update/mark/list/detail endpoints exist and domain invariants are mostly enforced in service layer (`TodoRestController`, `TodoService`, `DataService`).
4. **Error response shape is not fully consistent for all 400s**: custom `ErrorResponse` covers validation/domain exceptions, but query/path type mismatches are not explicitly handled in `GlobalExceptionHandler` (e.g., invalid `all` only asserted as 400 in tests).
5. **Good test structure with deterministic time control**: unit + integration coverage across controller/service/repository and fixed `Clock`; however, missing tests for a few high-risk API/edge behaviors (see section 6).

Runtime verification note:
- I could not re-run `./mvnw test` in this environment due missing Java and restricted Maven wrapper download. I used existing test artifacts in `service/target/surefire-reports` as evidence (all reported passing at the artifact generation time).

## 2. Requirements coverage

| Requirement | Evidence (file/endpoint/test) | Status | Notes |
| --- | --- | --- | --- |
| Todo has `description` | `Todo.description` (`service/src/main/java/com/demo/todo/model/Todo.java`) | Met | Also validated by DTO constraints. |
| Todo has `status` (`not done`/`done`/`past due`) | `TodoStatus` enum (`NOT_DONE`, `DONE`, `PAST_DUE`) | Partial | Semantic states exist, but serialized values differ from objective wording in `docs/Objective.md`. |
| Todo has `createdAt` | `Todo.createdAt`; set on create in `TodoService.createTodo` | Met | Uses `Instant`. |
| Todo has `dueAt` | `Todo.dueAt`; required in `CreateTodoRequest` | Met | Validated `@NotNull` + business rule `dueAt >= now`. |
| Todo has `doneAt` when marked done | `Todo.doneAt`; set in `DataService.markDone` | Met | Cleared on reopen (`markNotDone`). |
| Add item | `POST /todos` (`TodoRestController.createTodo`) | Met | Returns `201` + `Location`. |
| Change description | `PATCH /todos/{id}/description` | Met | Validated and persisted via `DataService.updateDescription`. |
| Mark as done | `POST /todos/{id}/done` | Met | Idempotent for already `DONE` in `DataService.markDone`. |
| Mark as not done | `POST /todos/{id}/not-done` | Met | Reopen rule for overdue done item enforced (`OverdueReopenForbiddenException`). |
| List not done items (optionally all) | `GET /todos?all=false|true`; `listTodos(@RequestParam(defaultValue="false") boolean all)` | Met | Default false returns `NOT_DONE` only; true returns all statuses. |
| Get specific item details | `GET /todos/{id}` | Met | Returns `TodoResponse` with all expected fields. |
| Auto-switch past due | `TodoRepository.markOverdueAsPastDue*` called in `DataService` before read/write ops | Partial | Works lazily on API access; not continuously automatic in background. |
| Forbid changing `past due` via REST | `DataService.ensureMutable` before updates | Met | Returns `409` through `GlobalExceptionHandler`. |
| Dockerized | `service/Dockerfile`, `docker-compose.yml` | Met | Multi-stage build; compose exposes `8080`. |
| H2 in-memory DB | `spring.datasource.url=jdbc:h2:mem:todosdb;DB_CLOSE_DELAY=-1` | Met | True in-memory mode. |
| Automatic tests | Unit + WebMvc + SpringBoot + DataJpa tests under `service/src/test/java` | Met | Good breadth for challenge scope. |
| No authentication | No Spring Security dependency/config (`service/pom.xml`, source scan) | Met | Requirement satisfied. |
| README: description + assumptions + tech stack + build/test/run | `README.md` sections present | Met | Includes assumptions and commands. |

## 3. API review

Implemented endpoints:
- `POST /todos`
  - Request: `CreateTodoRequest { description, dueAt }`
  - Response: `201 Created`, body `TodoResponse`, `Location: /todos/{id}`
  - Validation: `@NotBlank`, `@Size(max=1000)`, `@NotNull`; business check `dueAt >= now`
- `PATCH /todos/{id}/description`
  - Request: `UpdateDescriptionRequest { description }`
  - Response: `200 OK`, updated `TodoResponse`
- `POST /todos/{id}/done`
  - Response: `200 OK`, `TodoResponse`
  - Behavior: idempotent for already `DONE`
- `POST /todos/{id}/not-done`
  - Response: `200 OK`, `TodoResponse`
  - Behavior: blocks reopen of overdue `DONE` item (`409`)
- `GET /todos/{id}`
  - Response: `200 OK`, `TodoResponse`
- `GET /todos?all=false|true`
  - Response: `200 OK`, `TodosListResponse { items, meta {count, all} }`

RESTfulness/status codes/DTO comments:
- Positive:
  - `201 + Location` on create is correct.
  - Request/response DTOs are explicit and readable.
  - Domain conflict errors mapped to `409`.
- Notable issues:
  - Action-style endpoints (`/done`, `/not-done`) are pragmatic but less resource-oriented REST.
  - `GlobalExceptionHandler` does not cover all conversion/type mismatch errors; invalid query/path types may not return the standard `ErrorResponse` contract consistently.

## 4. Past-due automation & immutability rules

Implementation:
- Overdue sync is implemented via native update queries:
  - `TodoRepository.markOverdueAsPastDue(now)`
  - `TodoRepository.markOverdueAsPastDueById(id, now)`
- Sync is invoked in `DataService`:
  - Before single-read (`getByIdWithOverdueSync`)
  - Before list (`listWithOverdueSync`)
  - Before mutation (`updateDescription`, `markDone`, `markNotDone`)
- Immutability enforcement:
  - `DataService.ensureMutable` throws `PastDueImmutableException` when status is `PAST_DUE`
  - Mapped to `409` by `GlobalExceptionHandler`

Correctness notes:
- Good:
  - Overdue `NOT_DONE` gets transitioned before operations.
  - Mutation APIs correctly reject `PAST_DUE`.
  - `noRollbackFor` preserves overdue sync update even when conflict is returned.
- Risks:
  - No background scheduler means status can remain stale between API calls.
  - Rule interpretation (`DONE` tasks do not become `PAST_DUE`) is documented in README assumptions; acceptable if interviewer accepts this interpretation.

Suggested practical improvement:
- Keep request-time sync (simple) and add a lightweight scheduled sync (optional) to make “automatic” behavior true even without traffic.

## 5. Data model & persistence (H2)

Entity/mapping:
- `Todo` entity fields: `id`, `description`, `status`, `createdAt`, `dueAt`, `doneAt` (`service/src/main/java/com/demo/todo/model/Todo.java`)
- `status` stored as `VARCHAR` enum (`EnumType.STRING`)

Schema/migrations:
- Flyway migration `V1__create_todos_table.sql` defines:
  - `description VARCHAR(1000) NOT NULL`
  - `status` check constraint (`NOT_DONE`, `DONE`, `PAST_DUE`)
  - `done_at` consistency check (`DONE` requires `done_at`; others require null)
  - `due_at >= created_at` constraint
  - index `idx_todos_status_due_at (status, due_at)`

Persistence config:
- H2 in-memory URL: `jdbc:h2:mem:todosdb;DB_CLOSE_DELAY=-1`
- `spring.jpa.hibernate.ddl-auto=validate` with Flyway migration-driven schema

Observations:
- Constraints are solid for a challenge.
- No optimistic locking/version column (`@Version`), so concurrent updates are last-write-wins.

## 6. Tests

What exists:
- Unit tests:
  - `TodoServiceTest`
  - `DataServiceTest`
- Web slice tests:
  - `TodoRestControllerTest` (`@WebMvcTest`)
- Integration tests:
  - `TodoApiIntegrationTest` (`@SpringBootTest` + `MockMvc`)
  - `DataServiceIntegrationTest` (`@SpringBootTest`)
  - `TodoRepositoryIntegrationTest` (`@DataJpaTest`)

Quality assessment:
- Strengths:
  - Deterministic time handling via injected fixed `Clock` in tests.
  - Assertions cover key status transitions, conflict paths, and persistence side effects.
  - Layered test approach is good for challenge scope.
- Gaps:
  - Inconsistent-error-contract paths (query/path type mismatch) not asserted.
  - Idempotency not fully verified at API integration layer.
  - No concurrency behavior tests.

Evidence from existing reports:
- `service/target/surefire-reports/TEST-*.xml` show all suites passing (`73` tests total, `0` failures/errors).
- Re-run was not possible in this environment (no Java + restricted wrapper download).

5 concrete test cases to add:
1. `GET /todos?all=invalid` should return standardized `ErrorResponse` body (or intentionally documented alternate format).
2. `GET /todos/abc` and `PATCH /todos/abc/description` should produce consistent 400 payload contract.
3. API integration: call `POST /todos/{id}/done` twice; assert second call preserves original `doneAt`.
4. API integration: mixed statuses in DB, `GET /todos` default should exclude both `DONE` and `PAST_DUE` after sync.
5. Concurrency test: simultaneous `PATCH description` and `POST done` on same item; assert expected final state or add optimistic locking.

## 7. Docker & running locally

What works (by inspection):
- Docker setup exists and is coherent:
  - Multi-stage image build in `service/Dockerfile`
  - Compose service in `docker-compose.yml`
  - Healthcheck on `/actuator/health`
- Local run instructions are documented in `README.md`.

Exact commands discovered:
- From repo root (Docker):
  - `docker compose up --build`
- Manual build/test/run:
  - `cd service`
  - `./mvnw clean package`
  - `./mvnw test`
  - `./mvnw spring-boot:run`

Gaps/notes:
- I could not execute Docker or Java runtime commands in this environment, so Docker behavior is code-reviewed, not runtime-verified.
- A `.dockerignore` is not present; build context may include unnecessary files.

## 8. Code quality & maintainability

Architecture:
- Clean layering: controller -> orchestration service (`TodoService`) -> transactional data service (`DataService`) -> repository.
- Domain rules are centralized in service layer, not scattered in controllers.

Readability:
- Naming is mostly clear.
- Javadoc is extensive; in some places verbose for challenge scope but still readable.

Patterns and practices:
- Good:
  - Custom exceptions + centralized handler.
  - Validation annotations at DTO boundary.
  - Clock injection for testability.
  - Transaction boundaries on data service methods.
- Improvement areas:
  - Extend exception handling for type mismatch and generic unexpected errors to preserve a consistent error contract.
  - Consider reducing OpenAPI verbosity if interviewers prefer minimal challenge code footprint (current interface-based approach is technically clean).

Overengineering check:
- Slightly heavy documentation/OpenAPI layer for a coding challenge, but core implementation remains understandable and practical.

## 9. Edge cases & risk assessment

Validation:
- `description` blank/null/too long: handled (`CreateTodoRequest`, `UpdateDescriptionRequest` + tests).
- `dueAt` in past: handled in `TodoService.createTodo`.
- `dueAt` before `createdAt`: prevented by business logic and DB check constraint.
- Date parsing/timezone: uses `Instant` and UTC clock config; malformed timestamp -> `400`.

Idempotency:
- `markDone` twice: idempotent (no second save when already `DONE`).
- `markNotDone` twice: effectively idempotent on state (`NOT_DONE`, `doneAt=null`), though still saves.

Concurrency:
- No explicit optimistic locking/versioning. Concurrent writes can overwrite each other.

Error handling:
- Domain and validation exceptions return structured `ErrorResponse`.
- Potential inconsistency for parameter type mismatch/default framework exceptions.

Pagination/sorting:
- No pagination (not required).
- Sorting is deterministic (`createdAt ASC`), which is positive.

Time control:
- Strong: `Clock` is injected and overridden in tests.

Risk summary:
- **High**: status string contract ambiguity (`NOT_DONE` vs `"not done"`) depending on strict evaluator interpretation.
- **Medium**: request-driven (not continuous) past-due automation.
- **Medium**: inconsistent error payloads for some 400 scenarios.
- **Medium**: no optimistic locking for concurrent updates.

## 10. Commit history

Summary:
- Commit count on branch: `30` (`git rev-list --count HEAD`).
- History is mostly incremental and tells a generally coherent story from project setup -> API -> business rules -> docs/tests.

Message quality:
- Mixed quality:
  - Good: `add description field limit validation`, `add unit tests and a integration test`.
  - Weak/generic: `docs`, `cleanup`, `update readme`.

Red flags:
- One relatively large feature commit (`2842188 description, mark done and not done`, ~13 files, 881 insertions) reduces granularity for review.
- No obvious “broken build” commit pattern is visible from messages alone.

## 11. Suggested improvements (prioritized, small-to-large)

1. **Align status API contract to objective wording**
   - Either serialize status as `"not done"|"done"|"past due"` or explicitly document/confirm evaluator acceptance of enum form.
2. **Normalize all 400/404/409 responses to one error contract**
   - Add handlers for `MethodArgumentTypeMismatchException` and similar conversion failures.
3. **Add missing high-value integration tests**
   - Prioritize the 5 cases listed in section 6.
4. **Clarify “automatic past due” behavior in implementation**
   - Option A: keep lazy sync but document as “request-time automatic synchronization.”
   - Option B: add a small scheduler to transition overdue items periodically.
5. **Address concurrent update safety**
   - Add `@Version` optimistic locking on `Todo` and return conflict on stale updates.
6. **Challenge-polish improvements**
   - Add `.dockerignore` to reduce image build context and speed local Docker builds.
