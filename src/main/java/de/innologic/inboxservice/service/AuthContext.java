package de.innologic.inboxservice.service;

public record AuthContext(String companyId, String subjectId, boolean admin, boolean jwtAuthenticated) {
}
