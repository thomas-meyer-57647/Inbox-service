package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Antwort fuer interne Inbox-Zustellung.")
public record InternalCreateInboxMessagesResponseDto(
    @Schema(description = "Anzahl erzeugter Nachrichten.", example = "2")
    int createdCount,

    @ArraySchema(
        schema = @Schema(type = "string"),
        arraySchema = @Schema(description = "Erzeugte Message-IDs.", example = "[\"01JMSG1\",\"01JMSG2\"]")
    )
    List<String> messageIds
) {
}
