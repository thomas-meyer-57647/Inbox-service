package de.innologic.inboxservice.exception;

import de.innologic.inboxservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InboxServiceException.class)
    public ResponseEntity<ErrorResponse> handleInboxException(InboxServiceException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getHttpStatus()).body(new ErrorResponse(
            Instant.now(),
            ex.getHttpStatus().value(),
            ex.getErrorCode().name(),
            ex.getMessage(),
            request.getRequestURI(),
            request.getHeader("X-Correlation-Id"),
            null
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toDetail)
            .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            ErrorCode.VALIDATION_FAILED.name(),
            "Validation failed",
            request.getRequestURI(),
            request.getHeader("X-Correlation-Id"),
            details
        ));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleConstraintViolation(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            ErrorCode.VALIDATION_FAILED.name(),
            "Validation failed",
            request.getRequestURI(),
            request.getHeader("X-Correlation-Id"),
            List.of(ex.getMessage())
        ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            ErrorCode.OPTIMISTIC_LOCK_FAILED.name(),
            "Concurrent update detected. Please retry.",
            request.getRequestURI(),
            request.getHeader("X-Correlation-Id"),
            null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorCode.UNEXPECTED_ERROR.name(),
            ex.getMessage(),
            request.getRequestURI(),
            request.getHeader("X-Correlation-Id"),
            null
        ));
    }

    private String toDetail(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
