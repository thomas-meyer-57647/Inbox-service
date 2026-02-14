package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Interner Request zum Zustellen von Inbox-Nachrichten.")
public record InternalInboxDeliveryRequest(
    @Schema(description = "Mandanten-ID.", example = "01JCOMPANY123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "companyId is required")
    String companyId,

    @Schema(description = "Aufrufender Quellservice.", example = "messaging-service", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "sourceService is required")
    String sourceService,

    @Schema(description = "Optionale Korrelations-ID fuer Tracing.", example = "c-123", nullable = true)
    String correlationId,

    @ArraySchema(
        schema = @Schema(implementation = InternalInboxMessageItem.class),
        minItems = 1,
        arraySchema = @Schema(description = "Mindestens eine zuzustellende Nachricht.")
    )
    @NotEmpty(message = "messages must contain at least one element")
    List<@Valid InternalInboxMessageItem> messages
) {
}
