package com.demo.todo.service;

import com.demo.todo.exception.OverdueReopenForbiddenException;
import com.demo.todo.exception.PastDueImmutableException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataService {

    private final TodoRepository todoRepository;

    public DataService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional
    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    @Transactional(readOnly = true)
    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    @Transactional
    public Optional<Todo> getByIdWithOverdueSync(Long id, Instant now) {
        todoRepository.markOverdueAsPastDueById(id, now);
        return todoRepository.findById(id);
    }

    @Transactional
    public List<Todo> listWithOverdueSync(boolean all, Instant now) {
        todoRepository.markOverdueAsPastDue(now);
        if (all) {
            return todoRepository.findAllByOrderByCreatedAtAsc();
        }
        return todoRepository.findByStatusOrderByCreatedAtAsc(TodoStatus.NOT_DONE);
    }

    @Transactional(noRollbackFor = PastDueImmutableException.class)
    public Todo updateDescription(Long id, String description, Instant now) {
        todoRepository.markOverdueAsPastDueById(id, now);
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
        ensureMutable(todo);
        todo.setDescription(description);
        return todoRepository.save(todo);
    }

    @Transactional(noRollbackFor = PastDueImmutableException.class)
    public Todo markDone(Long id, Instant now) {
        todoRepository.markOverdueAsPastDueById(id, now);
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
        ensureMutable(todo);
        if (todo.getStatus() == TodoStatus.NOT_DONE) {
            todo.setStatus(TodoStatus.DONE);
            todo.setDoneAt(now);
            return todoRepository.save(todo);
        }
        return todo;
    }

    @Transactional(noRollbackFor = {PastDueImmutableException.class, OverdueReopenForbiddenException.class})
    public Todo markNotDone(Long id, Instant now) {
        todoRepository.markOverdueAsPastDueById(id, now);
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
        ensureMutable(todo);
        if (todo.getStatus() == TodoStatus.DONE) {
            if (todo.getDueAt().isBefore(now)) {
                throw new OverdueReopenForbiddenException();
            }
            todo.setStatus(TodoStatus.NOT_DONE);
            todo.setDoneAt(null);
        } else {
            todo.setDoneAt(null);
        }
        return todoRepository.save(todo);
    }

    private void ensureMutable(Todo todo) {
        if (todo.getStatus() == TodoStatus.PAST_DUE) {
            throw new PastDueImmutableException();
        }
    }
}
