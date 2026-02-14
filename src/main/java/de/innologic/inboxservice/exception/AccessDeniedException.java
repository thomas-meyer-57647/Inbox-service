package de.innologic.inboxservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends InboxServiceException {

    public AccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, message);
    }
}
