package de.innologic.inboxservice.service;

import de.innologic.inboxservice.exception.AccessDeniedException;
import de.innologic.inboxservice.exception.TokenInvalidException;
import de.innologic.inboxservice.exception.ValidationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class AuthContextResolver {

    public static final String HEADER_COMPANY_ID = "X-Company-Id";
    public static final String HEADER_SUBJECT_ID = "X-Subject-Id";
    public static final String HEADER_ROLE = "X-Role";
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ROLES_CLAIM = "roles";

    public AuthContext resolveUserContext(String companyId, String subjectId, String role) {
        Jwt jwt = resolveJwt();
        if (jwt != null) {
            String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);
            String subject = jwt.getSubject();
            if (isBlank(tenantId) || isBlank(subject)) {
                throw new TokenInvalidException("JWT must contain tenant_id and subject claims");
            }
            return new AuthContext(tenantId, subject, isAdmin(role, jwt), true);
        }

        if (isBlank(companyId) || isBlank(subjectId)) {
            throw new ValidationException("X-Company-Id and X-Subject-Id headers are required");
        }
        return new AuthContext(companyId, subjectId, isAdmin(role, null), false);
    }

    public void assertInternalToken(String providedToken, String expectedToken) {
        if (isBlank(providedToken) || !providedToken.equals(expectedToken)) {
            throw new AccessDeniedException("Internal endpoint access denied");
        }
    }

    private Jwt resolveJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken();
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAdmin(String role, Jwt jwt) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }
        if (jwt == null) {
            return false;
        }
        Object claim = jwt.getClaim(ROLES_CLAIM);
        if (claim instanceof Iterable<?> iterable) {
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                if (isAdminValue(iterator.next())) {
                    return true;
                }
            }
        } else if (claim instanceof String roles) {
            for (String candidate : roles.split(",")) {
                if (isAdminValue(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAdminValue(Object value) {
        return value != null && "ADMIN".equalsIgnoreCase(value.toString().trim());
    }
}
