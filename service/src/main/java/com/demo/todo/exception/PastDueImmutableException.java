package com.demo.todo.exception;

/**
 * Domain exception raised when a write operation targets a past-due todo.
 */
public class PastDueImmutableException extends RuntimeException {

    /**
     * Creates an exception with the default immutable-state message.
     */
    public PastDueImmutableException() {
        super("Past due items cannot be modified.");
    }
}
