package de.innologic.inboxservice.exception;

import org.springframework.http.HttpStatus;

public class TenantMismatchException extends InboxServiceException {

    public TenantMismatchException(String message) {
        super(HttpStatus.FORBIDDEN, ErrorCode.TENANT_MISMATCH, message);
    }
}
