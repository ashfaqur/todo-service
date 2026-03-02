package com.demo.todo.service;

import com.demo.todo.exception.OverdueReopenForbiddenException;
import com.demo.todo.exception.PastDueImmutableException;
import com.demo.todo.exception.TodoNotFoundException;
import com.demo.todo.model.Todo;
import com.demo.todo.model.TodoStatus;
import com.demo.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Transaction boundary for todo persistence and state transitions.
 * <p>
 * Encapsulates repository calls, overdue synchronization, and mutability rules.
 */
@Service
public class DataService {

    private final TodoRepository todoRepository;

    /**
     * Creates a data service instance.
     *
     * @param todoRepository repository used for todo persistence operations
     */
    public DataService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    /**
     * Persists a todo entity.
     *
     * @param todo todo entity to persist
     * @return persisted todo instance
     */
    @Transactional
    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    /**
     * Finds a todo by id without overdue synchronization.
     *
     * @param id todo identifier
     * @return optional todo when found
     */
    @Transactional(readOnly = true)
    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    /**
     * Synchronizes overdue status for a single item and then loads it.
     *
     * @param id  todo identifier
     * @param now current timestamp used for overdue evaluation
     * @return optional synchronized todo when found
     */
    @Transactional
    public Optional<Todo> getByIdWithOverdueSync(Long id, Instant now) {
        todoRepository.markOverdueAsPastDueById(id, now);
        return todoRepository.findById(id);
    }

    /**
     * Synchronizes overdue statuses and returns a sorted todo list.
     *
     * @param all when {@code true}, returns all statuses; otherwise returns only {@code NOT_DONE}
     * @param now current timestamp used for overdue evaluation
     * @return sorted list of todos based on filter mode
     */
    @Transactional
    public List<Todo> listWithOverdueSync(boolean all, Instant now) {
        todoRepository.markOverdueAsPastDue(now);
        if (all) {
            return todoRepository.findAllByOrderByCreatedAtAsc();
        }
        return todoRepository.findByStatusOrderByCreatedAtAsc(TodoStatus.NOT_DONE);
    }

    /**
     * Synchronizes all overdue {@code NOT_DONE} todos to {@code PAST_DUE}.
     *
     * @param now current timestamp used for overdue evaluation
     * @return number of transitioned records
     */
    @Transactional
    public int syncOverdue(Instant now) {
        return todoRepository.markOverdueAsPastDue(now);
    }

    /**
     * Updates description for a mutable todo after overdue synchronization.
     *
     * @param id          todo identifier
     * @param description normalized description value
     * @param now         current timestamp used for overdue evaluation
     * @return updated todo entity
     * @throws TodoNotFoundException     when no todo exists for the given id
     * @throws PastDueImmutableException when the todo is {@code PAST_DUE}
     */
    @Transactional(noRollbackFor = PastDueImmutableException.class)
    public Todo updateDescription(Long id, String description, Instant now) {
        todoRepository.markOverdueAsPastDueById(id, now);
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
        ensureMutable(todo);
        todo.setDescription(description);
        return todoRepository.save(todo);
    }

    /**
     * Marks a mutable todo as done.
     *
     * @param id  todo identifier
     * @param now current timestamp used for overdue evaluation and {@code doneAt}
     * @return updated or unchanged todo entity when already done
     * @throws TodoNotFoundException     when no todo exists for the given id
     * @throws PastDueImmutableException when the todo is {@code PAST_DUE}
     */
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

    /**
     * Marks a mutable todo as not done.
     *
     * @param id  todo identifier
     * @param now current timestamp used for overdue evaluation
     * @return updated todo entity
     * @throws TodoNotFoundException           when no todo exists for the given id
     * @throws PastDueImmutableException       when the todo is {@code PAST_DUE}
     * @throws OverdueReopenForbiddenException when reopening an overdue {@code DONE} item
     */
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
