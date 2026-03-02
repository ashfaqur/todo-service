# Executive Summary

The project is a solid, mostly complete implementation of the coding challenge with clear layering, good use of DTOs/validation, and strong automated test coverage across unit, web, and integration levels.

Core required behaviors are implemented and mapped cleanly to REST endpoints. The main notable gap is the interpretation of "automatic overdue transition": overdue synchronization is request-driven (on read/write/list) rather than background/scheduled, so persisted state may remain stale until an API call touches it.

Additional assumptions are explicitly documented in README (e.g., no scheduler, no auth, no optimistic locking), which is positive for reviewer clarity.

# Functional Requirements

## Requirement-by-Requirement Status

| Requirement | Status | Reasoning | Relevant files |
|---|---|---|---|
| Add item | Fully implemented | `POST /todos` creates todo in `NOT_DONE`, sets `createdAt`, validates `dueAt >= now`, stores `doneAt=null`. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java`, `service/src/main/java/com/demo/todo/service/TodoService.java` |
| Update description | Fully implemented | `PATCH /todos/{id}/description` updates description, trims input, rejects missing/invalid IDs, and blocks past-due modification. | `service/src/main/java/com/demo/todo/service/DataService.java`, `service/src/main/java/com/demo/todo/exception/GlobalExceptionHandler.java` |
| Mark done | Fully implemented | `POST /todos/{id}/done` transitions `NOT_DONE -> DONE`, keeps idempotency for already `DONE`, sets `doneAt=now`, blocks `PAST_DUE`. | `service/src/main/java/com/demo/todo/service/DataService.java` |
| Mark not done | Fully implemented | `POST /todos/{id}/not-done` supports `DONE -> NOT_DONE`, clears `doneAt`, blocks `PAST_DUE`. Also adds extra rule forbidding reopen of overdue `DONE` items. | `service/src/main/java/com/demo/todo/service/DataService.java`, `service/src/main/java/com/demo/todo/exception/OverdueReopenForbiddenException.java` |
| Get not-done items (with `all=true` option) | Fully implemented | `GET /todos?all=false` returns only `NOT_DONE`; `all=true` returns all statuses. Includes metadata and creation-time sorting. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java`, `service/src/main/java/com/demo/todo/service/TodoService.java` |
| Get single item | Fully implemented | `GET /todos/{id}` returns full item fields and 404 when missing. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java`, `service/src/main/java/com/demo/todo/service/TodoService.java` |
| Automatic overdue transition | Partially implemented | Overdue sync is performed automatically during API operations (`get/list/mutation`) via repository update queries, but there is no scheduler/background process for time-based autonomous updates. | `service/src/main/java/com/demo/todo/service/DataService.java`, `service/src/main/java/com/demo/todo/repository/TodoRepository.java`, `README.md` |
| Forbid modifying past due items | Fully implemented | All mutation paths run overdue sync first, then enforce immutability via `PastDueImmutableException` mapped to `409`. | `service/src/main/java/com/demo/todo/service/DataService.java`, `service/src/main/java/com/demo/todo/exception/GlobalExceptionHandler.java` |

## Deviations / Assumptions

- Explicit assumption: overdue transition is request-time synchronization, not scheduled background processing (`README.md`).
- Additional business rule beyond objective: reopening an overdue `DONE` item (`DONE -> NOT_DONE`) is forbidden (`OverdueReopenForbiddenException`).
- Clock boundary choice is strict `< now` for overdue, so `dueAt == now` is still mutable/not overdue.

## Design Inconsistencies (Functional)

- `DataService#markNotDone` saves entity even when status already `NOT_DONE` (idempotent behavior but unnecessary write).
- Error handling is standardized for known exceptions, but there is no explicit catch-all handler for unexpected exceptions.

# Non-Functional Requirements

| Requirement | Status | Notes |
|---|---|---|
| Dockerization | Implemented | Multi-stage Dockerfile + root `docker-compose.yml` present. |
| H2 in-memory usage | Implemented | `spring.datasource.url=jdbc:h2:mem:todosdb;DB_CLOSE_DELAY=-1`. |
| Automatic tests present | Implemented | Unit + web + repository + integration tests present. |
| No authentication implemented | Implemented | No auth/security stack or filters configured. |
| README: service description | Implemented | Clear overview and endpoint summary. |
| README: assumptions | Implemented | Assumptions and out-of-scope constraints are documented. |
| README: tech stack | Implemented | Runtime/framework/library stack listed. |
| README: build instructions | Implemented | `./mvnw clean package`. |
| README: test instructions | Implemented | `./mvnw test`. |
| README: run instructions | Implemented | Local run and Docker run documented. |

# Architecture & Design

## Strengths

- Clear layering: controller -> orchestration service (`TodoService`) -> transactional data service (`DataService`) -> repository.
- Clean API boundary: DTOs for request/response and centralized exception-to-error mapping.
- Deterministic time model: `Clock` injection and `Instant` usage improve testability.
- Good DB/domain alignment: Flyway schema constraints enforce status and `doneAt` consistency.
- Overdue sync implementation uses set-based SQL updates, not per-row iteration.

## Potential Design Improvements

- Add optional scheduled overdue sync (or document request-driven sync as final intended behavior if acceptable for challenge scope).
- Introduce optimistic locking (`@Version`) for mutation conflict safety under concurrency.
- Consider reducing redundant writes in idempotent paths (e.g., `markNotDone` on already `NOT_DONE`).
- Add a generic exception handler to keep error payload shape consistent for unexpected failures.
- If data size grows, consider indexes aligned with query order/filter patterns (`status + created_at`, possibly `created_at`).

## Over-Engineering Risks

- Current architecture is mostly appropriate; slight complexity overhead exists from splitting `TodoService` and `DataService` in a small challenge project, but it remains understandable and maintainable.

## Missing Edge-Case Handling

- Concurrent update conflicts are not detected (last-write-wins).
- No pagination for list endpoint.
- No explicit API-level tests for all mutation conflict paths at full-stack level (some are unit-only).

# Testing

## Coverage Overview

- Unit tests:
  - `TodoServiceTest`: orchestration, input rule for due date, mapping, propagation.
  - `DataServiceTest`: state transitions, idempotency, conflict/not-found behavior.
- Controller tests:
  - `TodoRestControllerTest`: endpoint status codes, validation errors, standardized payloads.
- Integration tests:
  - `TodoApiIntegrationTest`: selected end-to-end API flows and error payloads.
  - `TodoRepositoryIntegrationTest`: custom query behavior and sorting.
  - `DataServiceIntegrationTest`: transactional sync + mutation rule behavior with DB.

## Deterministic Time Usage

- Strong: fixed `Clock` in service/controller/API tests.
- Service/data methods rely on injected/passed `Instant`, making boundary tests deterministic.

## Edge Cases / Scenarios

- Covered:
  - `dueAt == now` boundary
  - `dueAt < now` rejection on create
  - idempotency (`markDone` and `markNotDone`)
  - invalid params / malformed body
  - past-due immutability conflicts
- Coverage gaps:
  - No explicit concurrency/conflict race tests.
  - Limited full-stack tests for some mutation conflict permutations.
  - No performance/load-oriented tests.

## Execution Note

- I did **not** run tests in this review pass per your request.
- Existing surefire artifacts under `service/target/surefire-reports` indicate a prior passing run (all listed suites show `failures=0`, `errors=0`), but that is historical evidence, not a fresh execution for this review.

# Remaining Work

- Clarify and finalize interpretation of "automatic overdue transition":
  - If request-driven sync is acceptable, keep as-is and emphasize this explicitly in objective assumptions.
  - If true autonomous transition is expected, add scheduled/background synchronization.
- Decide whether overdue `DONE -> NOT_DONE` rejection is a required rule or an extra assumption; document rationale clearly.
- Add a small set of additional integration tests for remaining mutation conflict permutations and end-to-end idempotency.
- Consider adding optimistic locking and testing concurrent modification scenarios.

# Final Assessment

- Overall compliance score: **8.8 / 10**
- Code quality score: **8.6 / 10**
- Readiness for submission: **Minor fixes**

## Verdict Summary

- Functional scope is largely complete and well implemented.
- Architecture is clean, readable, and testable with good separation of concerns.
- Automated tests are strong for challenge scale, with clear deterministic-time strategy.
- Main review risk is interpretation of overdue automation (request-driven vs autonomous scheduler).
- A few targeted clarifications/tests would make submission more robust.
