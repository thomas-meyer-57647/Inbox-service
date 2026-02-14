package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Kompakte Darstellung einer Inbox-Nachricht in Listenansichten.")
public record InboxMessageListItemResponse(
    @Schema(description = "Eindeutige Message-ID.", example = "01JMSG123")
    String messageId,

    @Schema(description = "Nachrichtentitel.", example = "Ticket aktualisiert")
    String title,

    @Schema(description = "Kategorie der Nachricht.", example = "TRANSACTIONAL")
    String category,

    @Schema(description = "Severity der Nachricht.", example = "INFO")
    String severity,

    @Schema(description = "Status der Nachricht.", example = "UNREAD")
    String status,

    @Schema(description = "Erstellungszeitpunkt (UTC).", example = "2026-02-14T09:00:00Z")
    Instant createdAt
) {
}
