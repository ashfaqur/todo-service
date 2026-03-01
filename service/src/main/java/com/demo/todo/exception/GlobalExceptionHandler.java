package com.demo.todo.exception;

import com.demo.todo.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TodoNotFoundException.class)
    public ErrorResponse handleTodoNotFound(TodoNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("TODO_NOT_FOUND", ex.getMessage(), request);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidTodoInputException.class)
    public ErrorResponse handleInvalidTodoInput(InvalidTodoInputException ex, HttpServletRequest request) {
        return buildErrorResponse("INVALID_REQUEST", ex.getMessage(), request);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PastDueImmutableException.class)
    public ErrorResponse handlePastDueImmutable(PastDueImmutableException ex, HttpServletRequest request) {
        return buildErrorResponse("PAST_DUE_IMMUTABLE", ex.getMessage(), request);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OverdueReopenForbiddenException.class)
    public ErrorResponse handleOverdueReopenForbidden(
            OverdueReopenForbiddenException ex, HttpServletRequest request) {
        return buildErrorResponse("OVERDUE_REOPEN_FORBIDDEN", ex.getMessage(), request);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildErrorResponse("INVALID_REQUEST", message, request);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildErrorResponse("INVALID_REQUEST", "Malformed request body", request);
    }

    private ErrorResponse buildErrorResponse(String error, String message, HttpServletRequest request) {
        return new ErrorResponse(error, message, request.getRequestURI(), Instant.now(clock));
    }
}
