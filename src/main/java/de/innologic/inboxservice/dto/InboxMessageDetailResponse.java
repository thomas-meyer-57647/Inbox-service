package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Vollstaendige Detaildarstellung einer Inbox-Nachricht.")
public record InboxMessageDetailResponse(
    @Schema(description = "Eindeutige Message-ID.", example = "01JMSG123")
    String messageId,

    @Schema(description = "Mandanten-ID.", example = "01JCOMPANY123")
    String companyId,

    @Schema(description = "Empfaenger-Benutzer-ID.", example = "01JUSER123")
    String recipientUserId,

    @Schema(description = "Nachrichtentitel.", example = "Ticket aktualisiert")
    String title,

    @Schema(description = "Nachrichteninhalt.", example = "Dein Ticket #4711 wurde bearbeitet.")
    String body,

    @Schema(description = "Kategorie der Nachricht.", example = "TRANSACTIONAL")
    String category,

    @Schema(description = "Severity der Nachricht.", example = "INFO")
    String severity,

    @Schema(description = "Status der Nachricht.", example = "UNREAD")
    String status,

    @Schema(description = "Quellservice.", example = "messaging-service")
    String sourceService,

    @Schema(description = "Optionale Korrelations-ID.", example = "c-123", nullable = true)
    String correlationId,

    @Schema(description = "Optionales Ablaufdatum (UTC).", example = "2026-05-01T00:00:00Z", nullable = true)
    Instant expiresAt,

    @Schema(description = "Read-Zeitpunkt (UTC).", example = "2026-02-14T11:00:00Z", nullable = true)
    Instant readAt,

    @Schema(description = "Benutzer-ID, der die Nachricht gelesen hat.", example = "01JUSER123", nullable = true)
    String readBy,

    @ArraySchema(
        schema = @Schema(implementation = ActionDto.class),
        arraySchema = @Schema(description = "Optionale UI-Aktionen.", example = "[{\"label\":\"Ticket oeffnen\",\"url\":\"/tickets/4711\",\"actionType\":\"LINK\"}]")
    )
    List<ActionDto> actions,

    @ArraySchema(
        schema = @Schema(implementation = AttachmentRefDto.class),
        arraySchema = @Schema(description = "Attachment-Referenzen.", example = "[{\"fileId\":\"01JFILE123\",\"filename\":\"report.pdf\",\"mimeType\":\"application/pdf\",\"sizeBytes\":32768}]")
    )
    List<AttachmentRefDto> attachments,

    @Schema(description = "Erstellt am (UTC).", example = "2026-02-14T09:00:00Z")
    Instant createdAt,

    @Schema(description = "Erstellt von.", example = "messaging-service")
    String createdBy,

    @Schema(description = "Zuletzt geaendert am (UTC).", example = "2026-02-14T11:00:00Z")
    Instant modifiedAt,

    @Schema(description = "Zuletzt geaendert von.", example = "01JUSER123")
    String modifiedBy,

    @Schema(description = "In den Papierkorb verschoben am (UTC).", example = "2026-02-20T10:00:00Z", nullable = true)
    Instant trashedAt,

    @Schema(description = "In den Papierkorb verschoben von.", example = "01JUSER123", nullable = true)
    String trashedBy,

    @Schema(description = "Optimistic-Locking-Version.", example = "1")
    Long version
) {
}
