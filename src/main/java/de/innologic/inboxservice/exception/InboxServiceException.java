package de.innologic.inboxservice.exception;

import org.springframework.http.HttpStatus;

public class InboxServiceException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    public InboxServiceException(HttpStatus httpStatus, ErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
