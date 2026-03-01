# Development Notes

- [Development Notes](#development-notes)
  - [Objective](#objective)
  - [Data model](#data-model)
  - [API Design](#api-design)
  - [Update during get all](#update-during-get-all)
  - [Project setup](#project-setup)
  - [APIs](#apis)

## Objective

P: Analyze and overview the following challenge objective.

Todo Item:
- id 
- description (string)
- status enum: NOT_DONE, DONE, PAST_DUE
- createdAt (datetime, set on create)
- dueAt (datetime, provided by client on create)
- doneAt (datetime, nullable; set when marking done; cleared when marking not done)

Behavior:

1. On create
- createdAt = now
- status = NOT_DONE 
  but to set PAST_DUE immediately if dueAt < now (document it).

1. Past due rule (automatic)
- If status == NOT_DONE and now > dueAt -> becomes PAST_DUE.

If an item is DONE, do you ever make it PAST_DUE later? Most interpretations: no (done is terminal). Document.

3. Forbidden updates
Any “change description”, “mark done”, “mark not done” must be rejected if current status is PAST_DUE.

4. doneAt semantics
Mark DONE -> doneAt -> now
Mark NOT_DONE -> doneAt -> null

5. The “automatic past due” requirement

Compute status on read

When fetch items (list or details):

- check now > dueAt for NOT_DONE items
- update status to PAST_DUE (persist)

Pros: no scheduler complexity, always correct.
Cons: status changes happen on read, not continuously — but still “automatic” in a practical sense.

6. Edge cases 

- Creating with dueAt in the past (reject vs immediately PAST_DUE)
- Marking done exactly at due time (now == dueAt): define overdue as now > dueAt (usually).
- Changing description to empty / too long (add basic validation)
- Marking done multiple times (idempotency)
- Marking not done when already not done (idempotency)
- Past due item: all mutation endpoints must block it consistently
- Timezone handling (use Instant in persistence; accept ISO-8601 input)

## Data model

P: Define sql schema for data store table

```sql
CREATE TABLE todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    due_at TIMESTAMP NOT NULL,
    done_at TIMESTAMP NULL,

-- check enum
    CONSTRAINT check_status
        CHECK (status IN ('NOT_DONE', 'DONE', 'PAST_DUE')),

-- check done status and when done
    CONSTRAINT check_done_at_consistency
        CHECK (
          (status = 'DONE' AND done_at IS NOT NULL)
          OR
          (status <> 'DONE' AND done_at IS NULL)
        )

-- check due is after creation time
    CONSTRAINT check_due_after_created
        CHECK (due_at >= created_at)
);

-- index on status for performant retrival of not done
CREATE INDEX idx_todos_status ON todos(status);
```

## API Design

P: Dive in detail for API design

Base Path: /todos

Timestamp format: ISO-8601 with timezone
- "2026-03-01T10:00:00Z"
- "2026-03-01T11:00:00+01:00"

Status: NOT_DONE | DONE | PAST_DUE

The service should evaluate and persist overdue transitions (NOT_DONE→PAST_DUE)
on every read and before every write to prevent modifications after past due.

Todo representation (response DTO):

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
Error Response:

```json
{
  "error": "PAST_DUE_IMMUTABLE",
  "message": "Past due items cannot be modified.",
  "path": "/todos/123/done",
  "timestamp": "2026-02-28T09:40:00Z"
}
```

1. Add item


POST /todos

```json
{
  "description": "Pay rent",
  "dueAt": "2026-03-01T10:00:00Z"
}
```

-> 201 Created

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

-> 400 Bad Request

- description missing/blank
- dueAt missing/invalid format
- optional: dueAt < now either reject 400

2. Update description

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

3. Mark done

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

4. Mark not done

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

5. Get details

GET /todos/{id}

- If item is overdue and still stored as NOT_DONE, compute, persist and return it as PAST_DUE

-> 200 OK

```json
{
  "id": 123,
  "description": "Pay rent",
  "status": "PAST_DUE",
  "createdAt": "2026-02-28T09:30:00Z",
  "dueAt": "2026-02-28T09:35:00Z",
  "doneAt": null
}
```
-> 404 Not Found


6. Get all not-done (or all)

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

## Update during get all 

Bulk update during get all api call.

First update the DB
```sql
UPDATE todos
SET status = 'PAST_DUE'
WHERE status = 'NOT_DONE'
  AND due_at < CURRENT_TIMESTAMP;
```
then select the relevent items
```sql
SELECT * FROM todos WHERE status = 'NOT_DONE';
```
or
```sql
SELECT * FROM todos;
```

Consider pagination later if there is time.

Also a composite index will make the update performant

```sql
CREATE INDEX idx_todos_status_due_at ON todos(status, due_at);
```

## Project setup

Setup the initial spring boot project

## APIs

Creates rest api endpoints for post create and get single todo


