package com.demo.todo.exception;

public class OverdueReopenForbiddenException extends RuntimeException {

    public OverdueReopenForbiddenException() {
        super("Overdue done items cannot be reopened.");
    }
}
