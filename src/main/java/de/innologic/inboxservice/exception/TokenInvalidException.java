package de.innologic.inboxservice.exception;

import org.springframework.http.HttpStatus;

public class TokenInvalidException extends InboxServiceException {

    public TokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_INVALID, message);
    }
}
