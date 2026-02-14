package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Attachment-Referenz. Es werden keine Binaerdaten im Service gespeichert.")
public record AttachmentRefDto(
    @Schema(description = "Datei-ID im Fileservice.", example = "01JFILE123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "fileId is required")
    String fileId,

    @Schema(description = "Dateiname.", example = "report.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "filename is required")
    String filename,

    @Schema(description = "MIME-Typ.", example = "application/pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "mimeType is required")
    String mimeType,

    @Schema(description = "Dateigroesse in Byte.", example = "32768", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "sizeBytes is required")
    Long sizeBytes
) {
}
