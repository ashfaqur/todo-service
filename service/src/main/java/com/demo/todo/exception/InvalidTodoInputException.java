package com.demo.todo.exception;

public class InvalidTodoInputException extends RuntimeException {

    public InvalidTodoInputException(String message) {
        super(message);
    }
}
