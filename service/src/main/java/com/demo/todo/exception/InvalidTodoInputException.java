package com.demo.todo.exception;

/**
 * Domain exception for invalid todo input accepted by request parsing but
 * rejected by business validation rules.
 */
public class InvalidTodoInputException extends RuntimeException {

    /**
     * Creates an exception with a validation message.
     *
     * @param message validation error message
     */
    public InvalidTodoInputException(String message) {
        super(message);
    }
}
