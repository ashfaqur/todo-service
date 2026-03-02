# API Usage Examples

`BASE_URL` defaults to local service:

```bash
BASE_URL=http://localhost:8080
```

## 1. Create a todo

```bash
curl -i -X POST "$BASE_URL/todos" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Pay rent",
    "dueAt": "2026-03-15T10:00:00Z"
  }'
```

## 2. Get a todo by id

```bash
curl -i "$BASE_URL/todos/1"
```

## 3. List not-done todos (default)

```bash
curl -i "$BASE_URL/todos"
```

## 4. List all todos

```bash
curl -i "$BASE_URL/todos?all=true"
```

## 5. Update description

```bash
curl -i -X PATCH "$BASE_URL/todos/1/description" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Pay rent (landlord)"
  }'
```

## 6. Mark done

```bash
curl -i -X POST "$BASE_URL/todos/1/done"
```

## 7. Mark not done

```bash
curl -i -X POST "$BASE_URL/todos/1/not-done"
```

## 8. Example invalid create (400)

```bash
curl -i -X POST "$BASE_URL/todos" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "",
    "dueAt": "2026-03-15T10:00:00Z"
  }'
```

## 9. Example not found (404)

```bash
curl -i "$BASE_URL/todos/999999"
```
