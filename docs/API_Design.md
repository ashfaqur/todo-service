# API Design

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
