package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Einheitliches Fehlerformat der API.")
public record ErrorResponse(
    @Schema(description = "Fehlerzeitpunkt (UTC).", example = "2026-02-14T12:00:00Z")
    Instant timestamp,

    @Schema(description = "HTTP-Statuscode.", example = "400")
    int status,

    @Schema(description = "Stabiler fachlicher Fehlercode.", example = "INVALID_REQUEST")
    String errorCode,

    @Schema(description = "Menschenlesbare Fehlermeldung.", example = "Validation failed")
    String message,

    @Schema(description = "Request-Pfad.", example = "/api/v1/inbox/messages")
    String path,

    @Schema(description = "Korrelations-ID fuer Tracing.", example = "c-123", nullable = true)
    String correlationId,

    @ArraySchema(
        schema = @Schema(type = "string"),
        arraySchema = @Schema(description = "Optionale Detailinformationen, z. B. Validierungsfehler.", nullable = true, example = "[\"title is required\"]")
    )
    List<String> details
) {
}
