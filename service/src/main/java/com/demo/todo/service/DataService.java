package com.demo.todo.service;

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
}
