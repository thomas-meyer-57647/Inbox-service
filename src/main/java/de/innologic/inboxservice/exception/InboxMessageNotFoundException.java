package de.innologic.inboxservice.exception;

import org.springframework.http.HttpStatus;

public class InboxMessageNotFoundException extends InboxServiceException {

    public InboxMessageNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ErrorCode.INBOX_MESSAGE_NOT_FOUND, message);
    }
}
