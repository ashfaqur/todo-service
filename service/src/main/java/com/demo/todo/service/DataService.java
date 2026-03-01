package com.demo.todo.service;

import com.demo.todo.model.Todo;
import com.demo.todo.repository.TodoRepository;
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
}
