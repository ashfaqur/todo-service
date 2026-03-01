package com.demo.todo.exception;

public class PastDueImmutableException extends RuntimeException {

    public PastDueImmutableException() {
        super("Past due items cannot be modified.");
    }
}
