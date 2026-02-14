package de.innologic.inboxservice.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends InboxServiceException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message);
    }
}
