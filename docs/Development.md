# Development

In a production workflow, this work would normally be delivered through feature branches, pull-request review, and CI gates before merge.
For this exercise, commits were made directly on `main` to keep iteration fast, so the history reflects incremental refinement rather than polished PR batches.

This document summarizes the project as a compact milestone narrative derived from the commit history and file-level changes.

## Milestones

1. **Requirements and scope alignment**
   Initial documentation established
   - objective
   - expected behavior
   - delivery boundaries
   before code was introduced.

   The main design decision was to lock scope early,
   so later implementation could be validated against explicit rules.

2. **Initial API and schema design**
   API contracts and schema drafts were created up front, including
   - status model
   - timestamp handling
   - error response shape.

3. **Edge-case analysis and rule framing**
   Follow-up planning focused on lifecycle edge cases such as
   - overdue transitions
   - immutability
   - idempotent state changes.

4. **Project scaffolding and local runtime setup**
   The Spring Boot baseline, Maven wrapper files, repository hygiene (`.gitignore`, wrapper metadata), and Docker runtime setup were introduced and corrected.
   The core decision was to prioritize reproducible local execution and stable build bootstrapping before feature work.

5. **Database configuration**
   Flyway migration and application property updates established schema management, validation behavior, and development logging defaults.

6. **Core create/read flow implementation**
   The first functional slice delivered `POST /todos` and `GET /todos/{id}` with layered controller/service/repository design, DTOs, and initial exception handling.
   A key design choice was injecting `Clock` for deterministic time-dependent logic and testability.

7. **Service-layer refactor and data access abstraction**
   A dedicated `DataService` layer was introduced to centralize persistence-oriented logic and reduce coupling in `TodoService`.
   This improved maintainability by placing data synchronization and mutation rules behind a focused abstraction.

8. **List endpoint and overdue-state enforcement**
   The list API was added (`GET /todos?all=...`) with structured list DTOs and repository queries sorted by creation time.
   Overdue synchronization logic was introduced to persist `NOT_DONE -> PAST_DUE` transitions during read flows, keeping responses consistent with business rules.

9. **Update and completion workflow finalization**
   Remaining mutation endpoints were implemented: update description, mark done, and mark not-done.
   Explicit conflict handling (`PAST_DUE` immutability and overdue reopen restrictions) completed the lifecycle contract and protected invalid transitions.

10. **Test hardening and behavioral verification**
    Test coverage expanded across controller, service, repository, and integration scopes as features were introduced and refactored.
    The main technical concern in this phase was validating state transitions and edge cases (overdue sync, idempotency, and conflict paths) under deterministic time.

11. **Documentation consolidation and reviewer packaging**
    README and supporting documentation were iteratively refined, including document renames/reorganization and dedicated AI-usage notes.
    This phase focused on reviewer clarity and project traceability without changing runtime behavior.

12. **OpenAPI documentation**
    API documentation was formalized with Springdoc integration, centralized endpoint annotations, and reusable example payloads.
    The intent was to keep controller code focused on runtime behavior while maintaining complete, reviewer-friendly API discoverability.

13. **Scheduler implementation**
    A periodic overdue synchronization job was introduced to complement request-time status synchronization.
    Configurable scheduler controls (`enabled`, `fixed-delay`) were added to support both production runtime behavior and deterministic test setups.
