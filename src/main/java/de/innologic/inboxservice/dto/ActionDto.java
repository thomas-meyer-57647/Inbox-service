package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Aktion innerhalb einer Inbox-Nachricht, z. B. ein Link-Button.")
public record ActionDto(
    @Schema(description = "Anzeigetext des Buttons oder Links.", example = "Ticket oeffnen", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "label is required")
    String label,

    @Schema(description = "Ziel-URL fuer die Aktion.", example = "/tickets/4711", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "url is required")
    String url,

    @Schema(description = "Typ der Aktion.", example = "LINK", requiredMode = Schema.RequiredMode.REQUIRED, implementation = ActionType.class)
    @NotNull(message = "actionType is required")
    ActionType actionType
) {
}
