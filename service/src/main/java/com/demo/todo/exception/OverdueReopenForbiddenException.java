package com.demo.todo.exception;

/**
 * Domain exception raised when an overdue completed todo is reopened.
 */
public class OverdueReopenForbiddenException extends RuntimeException {

    /**
     * Creates an exception with the default reopen-forbidden message.
     */
    public OverdueReopenForbiddenException() {
        super("Overdue done items cannot be reopened.");
    }
}
