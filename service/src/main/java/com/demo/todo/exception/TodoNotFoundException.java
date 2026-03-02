package com.demo.todo.exception;

/**
 * Domain exception raised when a todo cannot be found by identifier.
 */
public class TodoNotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception for the provided id.
     *
     * @param id missing todo identifier
     */
    public TodoNotFoundException(Long id) {
        super("Todo not found for id: " + id);
    }
}
