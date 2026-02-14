package de.innologic.inboxservice.service;

import de.innologic.inboxservice.exception.AccessDeniedException;
import de.innologic.inboxservice.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class AuthContextResolver {

    public static final String HEADER_COMPANY_ID = "X-Company-Id";
    public static final String HEADER_SUBJECT_ID = "X-Subject-Id";
    public static final String HEADER_ROLE = "X-Role";

    public AuthContext resolveUserContext(String companyId, String subjectId, String role) {
        if (isBlank(companyId) || isBlank(subjectId)) {
            throw new ValidationException("X-Company-Id and X-Subject-Id headers are required");
        }
        boolean admin = "ADMIN".equalsIgnoreCase(role);
        return new AuthContext(companyId, subjectId, admin);
    }

    public void assertInternalToken(String providedToken, String expectedToken) {
        if (isBlank(providedToken) || !providedToken.equals(expectedToken)) {
            throw new AccessDeniedException("Internal endpoint access denied");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
