package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

@Schema(description = "Einzelne Nachricht innerhalb des internen Zustell-Requests.")
public record InternalInboxMessageItem(
    @Schema(description = "Empfaenger-Benutzer-ID.", example = "01JUSER123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "recipientUserId is required")
    String recipientUserId,

    @Schema(description = "Nachrichtentitel.", example = "Ticket aktualisiert", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "title is required")
    String title,

    @Schema(description = "Nachrichteninhalt.", example = "Dein Ticket #4711 wurde bearbeitet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "body is required")
    String body,

    @Schema(description = "Kategorie der Nachricht.", example = "TRANSACTIONAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "category is required")
    String category,

    @Schema(description = "Severity der Nachricht.", example = "INFO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "severity is required")
    String severity,

    @Schema(description = "Optionales Ablaufdatum (UTC).", example = "2026-05-01T00:00:00Z", nullable = true)
    Instant expiresAt,

    @ArraySchema(
        schema = @Schema(implementation = ActionDto.class),
        arraySchema = @Schema(description = "Optionale UI-Aktionen.", example = "[{\"label\":\"Ticket oeffnen\",\"url\":\"/tickets/4711\",\"actionType\":\"LINK\"}]")
    )
    List<@Valid ActionDto> actions,

    @ArraySchema(
        schema = @Schema(implementation = AttachmentRefDto.class),
        arraySchema = @Schema(description = "Optionale Attachment-Referenzen.", example = "[{\"fileId\":\"01JFILE123\",\"filename\":\"report.pdf\",\"mimeType\":\"application/pdf\",\"sizeBytes\":32768}]")
    )
    List<@Valid AttachmentRefDto> attachments
) {
}
