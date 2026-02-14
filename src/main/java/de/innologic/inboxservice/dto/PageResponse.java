package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Standardisierte Paging-Antwort.")
public record PageResponse<T>(
    @Schema(description = "Seitennummer (0-basiert).", example = "0")
    int page,

    @Schema(description = "Seitengroesse.", example = "50")
    int size,

    @Schema(description = "Gesamtanzahl Treffer.", example = "123")
    long total,

    @ArraySchema(arraySchema = @Schema(description = "Listenelemente der aktuellen Seite."))
    List<T> items
) {
}
