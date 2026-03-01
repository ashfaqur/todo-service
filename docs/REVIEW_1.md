# Executive Summary

The project implements the core to-do API with clear controller/service/repository separation, good use of DTOs, explicit DB schema constraints, and broad automated test coverage across unit, web, and data layers.  
Most functional requirements are met, including add/update/mark/list/get flows and mutation blocking for `PAST_DUE` items.

The main gaps are:
- Overdue transition is implemented as lazy sync during API operations (not continuous/background).
- `README.md` is effectively empty and does not satisfy required documentation content.
- Some edge-case handling and end-to-end integration coverage are still limited.

Test execution was not run in this review per request, so testing conclusions are based on static inspection of the test suite.

# Functional Requirements

| Requirement | Status | Reasoning | Evidence |
|---|---|---|---|
| Add item | Fully implemented | `POST /todos` exists, validates request body, sets `NOT_DONE`, `createdAt`, `doneAt=null`, and returns `201 Created` with `Location`. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java:23`, `service/src/main/java/com/demo/todo/service/TodoService.java:28`, `service/src/main/java/com/demo/todo/dto/CreateTodoRequest.java:7` |
| Update description | Fully implemented | `PATCH /todos/{id}/description` delegates to service; data layer syncs overdue status first and blocks `PAST_DUE`. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java:42`, `service/src/main/java/com/demo/todo/service/TodoService.java:61`, `service/src/main/java/com/demo/todo/service/DataService.java:49` |
| Mark done | Fully implemented | `POST /todos/{id}/done` implemented; idempotent when already `DONE`; blocks `PAST_DUE`. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java:49`, `service/src/main/java/com/demo/todo/service/DataService.java:59` |
| Mark not done | Fully implemented | `POST /todos/{id}/not-done` implemented; supports reopening `DONE` items and idempotent handling; blocks `PAST_DUE`. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java:55`, `service/src/main/java/com/demo/todo/service/DataService.java:73` |
| Get not-done items (with `all=true`) | Fully implemented | `GET /todos` defaults `all=false` and returns only `NOT_DONE`; `all=true` returns all statuses. Overdue sync runs first. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java:36`, `service/src/main/java/com/demo/todo/service/TodoService.java:52`, `service/src/main/java/com/demo/todo/service/DataService.java:40` |
| Get single item | Fully implemented | `GET /todos/{id}` exists and performs overdue sync before returning. | `service/src/main/java/com/demo/todo/controller/TodoRestController.java:30`, `service/src/main/java/com/demo/todo/service/TodoService.java:45`, `service/src/main/java/com/demo/todo/service/DataService.java:34` |
| Automatic overdue transition | Partially implemented | Overdue transition is automatic only when API operations occur (read/write triggers native update). No background/scheduled transition independent of requests. | `service/src/main/java/com/demo/todo/service/DataService.java:34`, `service/src/main/java/com/demo/todo/service/DataService.java:40`, `service/src/main/java/com/demo/todo/repository/TodoRepository.java:14` |
| Forbid modifying past due items | Fully implemented | Mutation endpoints sync overdue first, then reject `PAST_DUE` through centralized guard and 409 mapping. | `service/src/main/java/com/demo/todo/service/DataService.java:91`, `service/src/main/java/com/demo/todo/exception/GlobalExceptionHandler.java:39` |

## Deviations and assumptions

- Done-overdue reopen is explicitly forbidden (`OverdueReopenForbiddenException`), which is stricter than the base objective and should be documented as an assumption.  
  Evidence: `service/src/main/java/com/demo/todo/service/DataService.java:80`, `service/src/main/java/com/demo/todo/exception/OverdueReopenForbiddenException.java:5`
- Creating with `dueAt < now` is rejected with `400` instead of creating directly as `PAST_DUE` (valid design choice, but assumption).  
  Evidence: `service/src/main/java/com/demo/todo/service/TodoService.java:30`

# Non-Functional Requirements

| Requirement | Status | Reasoning | Evidence |
|---|---|---|---|
| Dockerization | Implemented | Multi-stage `Dockerfile` and compose file are present. | `service/Dockerfile:1`, `docker-compose.yml:1` |
| H2 in-memory usage | Implemented | H2 runtime dependency and in-memory JDBC URL configured. | `service/pom.xml:66`, `service/src/main/resources/application.properties:8` |
| Automatic tests present | Implemented | Unit, controller, repository integration, and service integration tests exist. | `service/src/test/java/com/demo/todo/service/TodoServiceTest.java:29`, `service/src/test/java/com/demo/todo/controller/TodoRestControllerTest.java:36`, `service/src/test/java/com/demo/todo/repository/TodoRepositoryIntegrationTest.java:14`, `service/src/test/java/com/demo/todo/service/DataServiceIntegrationTest.java:17` |
| No authentication implemented | Implemented | No Spring Security dependency/configuration or auth filters/endpoints are present. | `service/pom.xml:32` |
| README: service description | Missing | Root README only contains repository title. | `README.md:1` |
| README: assumptions | Missing | No assumptions documented in README. | `README.md:1` |
| README: tech stack | Missing | No stack/runtime/framework details in README. | `README.md:1` |
| README: build instructions | Missing | No build steps in README. | `README.md:1` |
| README: test instructions | Missing | No test execution instructions in README. | `README.md:1` |
| README: run instructions | Missing | No local run instructions in README. | `README.md:1` |

# Architecture & Design

## Strengths

- Clear layering and dependency direction (`controller -> service -> data/repository`).  
  Evidence: `service/src/main/java/com/demo/todo/controller/TodoRestController.java:15`, `service/src/main/java/com/demo/todo/service/TodoService.java:18`, `service/src/main/java/com/demo/todo/service/DataService.java:16`
- Transaction boundaries are centralized in `DataService`; mutation methods intentionally use `noRollbackFor` so overdue sync updates persist even on conflict responses.  
  Evidence: `service/src/main/java/com/demo/todo/service/DataService.java:49`, `service/src/main/java/com/demo/todo/service/DataService.java:73`
- Time handling is deterministic and testable via `Clock` injection.  
  Evidence: `service/src/main/java/com/demo/todo/config/TimeConfig.java:10`, `service/src/main/java/com/demo/todo/service/TodoService.java:23`, `service/src/main/java/com/demo/todo/exception/GlobalExceptionHandler.java:21`
- Good DB/domain alignment through schema constraints (`status`, `doneAt` consistency, `due_at >= created_at`).  
  Evidence: `service/src/main/resources/db/migration/V1__create_todos_table.sql:9`
- Query/index awareness for overdue updates and not-done reads via `(status, due_at)` index and focused native updates.  
  Evidence: `service/src/main/resources/db/migration/V1__create_todos_table.sql:23`, `service/src/main/java/com/demo/todo/repository/TodoRepository.java:14`

## Potential design improvements

- Clarify overdue model: if “automatic” should mean time-driven even without requests, add scheduler/background process; otherwise document lazy-sync behavior explicitly.
- Add validation for description length (`<=1000`) to align with DB column size and prevent DB-level failures leaking as 500.
- Consider handling `DataIntegrityViolationException` in `GlobalExceptionHandler` for cleaner client errors on DB constraint violations.
- Avoid unnecessary writes in idempotent `markNotDone` path when status is already `NOT_DONE` (`DataService` currently always saves there).
- Consider optimistic locking/version field if concurrent updates are in scope.

## Over-engineering risks

- `DataService` plus `TodoService` split is defensible, but currently thin and could be simplified if project scope remains small.
- `noRollbackFor` usage is intentional but subtle; requires clear documentation to avoid future transactional misunderstandings.

## Missing edge-case handling

- No explicit API-level test/handling for oversized description.
- No explicit malformed query-param tests for `all`.
- No full-stack API integration tests (controller + DB + real serialization in one test slice).

# Testing

## Coverage by type

- Unit tests (service/data layer): present and substantive.  
  Evidence: `service/src/test/java/com/demo/todo/service/TodoServiceTest.java:29`, `service/src/test/java/com/demo/todo/service/DataServiceTest.java:24`
- Controller tests: present via `@WebMvcTest` with error-contract assertions.  
  Evidence: `service/src/test/java/com/demo/todo/controller/TodoRestControllerTest.java:36`
- Integration tests: present for repository and service-with-DB behavior.  
  Evidence: `service/src/test/java/com/demo/todo/repository/TodoRepositoryIntegrationTest.java:14`, `service/src/test/java/com/demo/todo/service/DataServiceIntegrationTest.java:17`
- Deterministic time usage: good in unit/web tests via fixed clock; service integration passes explicit timestamps.
  Evidence: `service/src/test/java/com/demo/todo/service/TodoServiceTest.java:40`, `service/src/test/java/com/demo/todo/controller/TodoRestControllerTest.java:285`
- Idempotency tests: present for `markDone` and `markNotDone`.
  Evidence: `service/src/test/java/com/demo/todo/service/DataServiceTest.java:175`, `service/src/test/java/com/demo/todo/service/DataServiceTest.java:222`
- Conflict scenarios: covered (`PAST_DUE`, reopen-forbidden).
  Evidence: `service/src/test/java/com/demo/todo/service/DataServiceTest.java:191`, `service/src/test/java/com/demo/todo/service/DataServiceTest.java:251`, `service/src/test/java/com/demo/todo/controller/TodoRestControllerTest.java:273`
- Boundary condition tests: partially covered (`dueAt == now`, `dueAt < now`).
  Evidence: `service/src/test/java/com/demo/todo/service/TodoServiceTest.java:45`, `service/src/test/java/com/demo/todo/service/TodoServiceTest.java:95`, `service/src/test/java/com/demo/todo/service/DataServiceTest.java:269`

## Coverage gaps

- No end-to-end API integration tests validating automatic overdue transition through HTTP + DB.
- No tests for oversized description and DB constraint failure mapping.
- No tests for concurrency/conflict timing windows near due-date boundaries.
- Test suite was not executed in this review (per request), so pass/fail status is unverified here.

# Remaining Work

1. Complete `README.md` with:
   - service description
   - explicit assumptions (especially overdue strategy and reopen policy)
   - tech stack
   - build/test/run instructions
2. Decide and document interpretation of “automatic overdue transition”:
   - lazy sync on API access (current), or
   - continuous/background transition
3. Add validation and error mapping for description length/DB constraint violations.
4. Add at least one full integration test path (`POST/GET/LIST/PATCH`) against real DB to verify behavior end-to-end.
5. Optionally optimize idempotent `markNotDone` to avoid no-op writes.

# Final Assessment

- Overall compliance score: **7.8 / 10**
- Code quality score: **8.1 / 10**
- Readiness for submission: **Minor fixes**

Verdict:
- Core required API behavior is implemented with solid layering and transactional rigor.
- Mutation blocking for `PAST_DUE` is consistently enforced.
- Overdue transition strategy is practical but only partially meets a strict “automatic” interpretation.
- Test suite quality is strong for unit/web/data layers, with some integration and edge-case gaps.
- README/documentation is the largest clear blocker against full objective compliance.
