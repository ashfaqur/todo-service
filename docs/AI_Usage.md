# AI Use in This Project

Short description of how AI was used in this project.

## Summary

AI was used as a pair-programming assistant throughout this project.
- Used browser-based GPT v5.2 for design discussion and review.
- Used codex GPT v5.3 for code generation and review.
- Discussed the high-level design and break implementation into detailed, manageable steps.
- Larger feature development plans were decomposed into small, single-responsibility components.
- Code snippets were drafted with AI support. Then manually reviewed, refactored and improved.
- Codex agent was used to review implemented components and then manually refined.
- Components were then interconnected with AI support to resolve integration issues.
- This workflow reduced review complexity and helped avoid regressions.

## Details

GPT 5.2 browser based model and the Codex GPT 5.3 model were used in this project. Overall, the experience has been positive. For the most part, it can generate working code, depending on the complexity of the task and the limits of its training.

AI was not used to solve larger tasks in one shot. There are plenty of drawbacks in that approach. Without a clear plan, the generated code can become unnecessarily complex. If asked to redo the same task, it can produce a completely different implementation, which creates a lot of review overhead. Its knowledge of the latest library versions can also be imperfect, so it may use outdated syntax. This one-shot flow also consumes significant tokens and time.

What worked best was using AI to discuss the high-level approach first, then creating a detailed implementation plan broken into smaller components. From there, AI supported on each component and then on integration with the wider system. This led to a faster iteration cycle, lower review cognitive load, and better oversight, since manual improvements, refactoring and adjustments were still necessary in most cases. Codex AI was very useful for reviewing ongoing progress against the initial plan and helping prioritize upcoming tasks.

AI was also a great tool for learning new ideas and concepts. For this purpose, the browser-based model was preferable since it is more verbose and can describe a variety of different approaches, which facilitate learning.

My thinking is to use AI like a pair-programming buddy and guide it through the implementation.

## Example AI Prompts

Representative prompt-response examples from `docs/AI.md` showing how AI was used during planning, refinement, and review.

1. Initial Endpoint Scope and Layering
**Prompt:** Create an implementation plan for only `POST /todos` and `GET /todos/{id}` using a layered package structure (`controller`, `service`, `repository`, `model`, `dto`, `exception`) and proper error handling.

**AI output summary:** Proposed a phased implementation plan with explicit classes (controller, service, entity, DTOs, exceptions, global handler), scoped to only the two endpoints.

**Implementation influence:** Helped define the initial project structure and prevented scope creep during the first coding phase.

2. Deterministic Time Handling
**Prompt:** Refine the plan so `dueAt >= now` validation is deterministic in tests by injecting `Clock` and using `Instant.now(clock)`.

**AI output summary:** Recommended capturing `now` once per request path, injecting `Clock` into service and exception handling, and aligning validation with DB constraints.

**Implementation influence:** Drove consistent time handling in business logic and tests, reducing flaky time-based behavior.

3. Contract-First Error Handling
**Prompt:** Ensure responses and global error payloads match the API design, including validation and not-found behavior.

**AI output summary:** Defined a unified error model (`error`, `message`, `path`, `timestamp`) and exception-to-HTTP mappings for common failure paths.

**Implementation influence:** Improved consistency of API failure responses across endpoints and simplified controller code.

4. Overdue Sync on Reads and List Endpoint Design
**Prompt:** Add `GET /todos` with `all=true|false` and enforce overdue transitions by syncing `NOT_DONE -> PAST_DUE` before reads.

**AI output summary:** Proposed update-before-fetch flow for single and list reads, list response DTOs with metadata, and clear in/out-of-scope boundaries.

**Implementation influence:** Guided the final read behavior and introduced consistent list response structure.

5. Repository Update Strategy and Ordering
**Prompt:** Include DB-level overdue updates with transaction-safe patterns, `@Modifying(clearAutomatically = true)`, and default list ordering.

**AI output summary:** Recommended parameterized SQL update methods, persistence-context refresh safeguards, and sorted repository queries by `createdAt`.

**Implementation influence:** Informed repository method design and reduced risk of stale reads after bulk updates.

6. Write Endpoint State-Machine Expansion
**Prompt:** Plan `PATCH /todos/{id}/description`, `POST /todos/{id}/done`, and `POST /todos/{id}/not-done`, reusing overdue sync and enforcing `PAST_DUE` immutability.

**AI output summary:** Outlined endpoint semantics, idempotency behavior, transactional flow in `DataService`, new DTO/exception requirements, and test coverage additions.

**Implementation influence:** Shaped the write-side state transition logic and ensured conflict handling remained explicit.

7. Rule Change: Block Reopening Overdue DONE Items
**Prompt:** Adjust only `/not-done` so `DONE` items with `dueAt < now` return `409` with `OVERDUE_REOPEN_FORBIDDEN`, without changing overdue sync SQL.

**AI output summary:** Produced a targeted change plan covering check order, new exception mapping, and deterministic tests for the new transition rule.

**Implementation influence:** Enabled a focused state-machine refinement without broad architectural changes.

8. Structured Implementation Review Prompt
**Prompt:** Review the repository against the challenge objective and generate a structured `REVIEW.md` covering functional, non-functional, architecture, and testing status.

**AI output summary:** Generated a reviewer-oriented checklist and output structure emphasizing requirement coverage, gaps, and readiness assessment.

**Implementation influence:** Supported documentation quality and submission-readiness validation rather than feature development.

9. OpenAPI Documentation Generation and Refactor
**Prompt:** Add comprehensive OpenAPI documentation for all existing endpoints (metadata, request/response schemas, status codes, examples, and error responses) without changing runtime behavior, then refactor documentation bloat by moving annotations to an API interface and moving JSON examples into a dedicated constants class.

**AI output summary:** Proposed a two-step approach: first annotate DTOs and endpoints with complete OpenAPI metadata and examples, then separate docs concerns into `TodoApi` and `OpenApiExamples` while keeping controller logic minimal.

**Implementation influence:** Improved API discoverability in Swagger UI and produced a cleaner controller by separating documentation concerns from request-handling code.
