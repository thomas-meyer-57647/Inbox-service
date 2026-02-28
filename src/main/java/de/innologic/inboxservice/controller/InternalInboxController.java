package de.innologic.inboxservice.controller;

import de.innologic.inboxservice.dto.ErrorResponse;
import de.innologic.inboxservice.dto.InternalInboxDeliveryRequest;
import de.innologic.inboxservice.dto.InternalCreateInboxMessagesResponseDto;
import de.innologic.inboxservice.service.AuthContextResolver;
import de.innologic.inboxservice.service.InternalInboxDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inbox/messages")
@Tag(name = "Internal Inbox Delivery", description = "Interne Endpoint-Gruppe fuer Zustellung durch messaging-service")
@SecurityRequirement(name = "bearerAuth")
public class InternalInboxController {

    private final InternalInboxDeliveryService inboxService;
    private final AuthContextResolver authContextResolver;
    private final String expectedInternalToken;
    private final boolean legacyInternalTokenEnabled;

    public InternalInboxController(InternalInboxDeliveryService inboxService,
                                   AuthContextResolver authContextResolver,
                                   @Value("${inbox.security.internal-token}") String expectedInternalToken,
                                   @Value("${inbox.security.legacy-internal-token-enabled:false}") boolean legacyInternalTokenEnabled) {
        this.inboxService = inboxService;
        this.authContextResolver = authContextResolver;
        this.expectedInternalToken = expectedInternalToken;
        this.legacyInternalTokenEnabled = legacyInternalTokenEnabled;
    }

    @Operation(
        summary = "Inbox-Nachrichten intern zustellen (Single oder Batch)",
        description = "Erzeugt pro Empfaenger eine InboxMessage mit Status UNREAD. " +
            "Nur fuer service-to-service Aufrufe vorgesehen."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zustellung erfolgreich", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = InternalCreateInboxMessagesResponseDto.class),
            examples = @ExampleObject(
                name = "deliveryCreated",
                summary = "Eine Nachricht wurde erstellt",
                value = "{\"createdCount\":1,\"messageIds\":[\"01JMSG123\"]}"
            )
        )),
        @ApiResponse(responseCode = "400", description = "Ungueltiger Request", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "invalidRequest",
                summary = "Pflichtfeld fehlt",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":400,\"errorCode\":\"INVALID_REQUEST\",\"message\":\"sourceService is required\",\"path\":\"/api/v1/internal/inbox/messages\",\"correlationId\":\"c-123\",\"details\":[\"sourceService is required\"]}"
            )
        )),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "403", description = "Interner Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "wrongToken",
                summary = "Ungueltiger interner Token",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":403,\"errorCode\":\"INVALID_INTERNAL_TOKEN\",\"message\":\"Invalid internal token\",\"path\":\"/api/v1/internal/inbox/messages\",\"correlationId\":\"c-123\",\"details\":[]}"
            )
        )),
        @ApiResponse(responseCode = "409", description = "Optimistic-Lock-Konflikt", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "optimisticLock",
                summary = "Gleichzeitige Aenderung",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":409,\"errorCode\":\"OPTIMISTIC_LOCK_FAILED\",\"message\":\"Concurrent update detected. Please retry.\",\"path\":\"/api/v1/internal/inbox/messages\",\"correlationId\":\"c-123\",\"details\":[]}"
            )
        )),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "unexpected",
                summary = "Interner Fehler",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":500,\"errorCode\":\"UNEXPECTED_ERROR\",\"message\":\"Unexpected server error\",\"path\":\"/api/v1/internal/inbox/messages\",\"correlationId\":\"c-123\",\"details\":[]}"
            )
        ))
    })
    @PostMapping
    public InternalCreateInboxMessagesResponseDto deliver(
        @Parameter(description = "Interner Service-Token", required = true, example = "change-me")
        @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
        @Parameter(description = "Actor SubjectId fuer Audit. Wenn nicht gesetzt, wird sourceService verwendet.", required = false, example = "messaging-service")
        @RequestHeader(value = "X-Subject-Id", required = false) String subjectId,
        @RequestBody(
            required = true,
            description = "Interner Zustell-Request mit einer oder mehreren Nachrichten",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InternalInboxDeliveryRequest.class),
                examples = @ExampleObject(
                    name = "batchRequest",
                    summary = "Batch mit zwei Nachrichten",
                    value = "{\"companyId\":\"01JCOMPANY123\",\"sourceService\":\"messaging-service\",\"correlationId\":\"c-123\",\"messages\":[{\"recipientUserId\":\"01JUSER123\",\"title\":\"Ticket aktualisiert\",\"body\":\"Dein Ticket #4711 wurde bearbeitet.\",\"category\":\"TRANSACTIONAL\",\"severity\":\"INFO\",\"expiresAt\":\"2026-05-01T00:00:00Z\",\"actions\":[{\"label\":\"Ticket oeffnen\",\"url\":\"/tickets/4711\",\"actionType\":\"LINK\"}],\"attachments\":[{\"fileId\":\"01JABCFILE\",\"filename\":\"report.pdf\",\"mimeType\":\"application/pdf\",\"sizeBytes\":32768}]}]}"
                )
            )
        )
        @Valid @org.springframework.web.bind.annotation.RequestBody InternalInboxDeliveryRequest request
    ) {
        if (legacyInternalTokenEnabled && internalToken != null && !internalToken.isBlank()) {
            authContextResolver.assertInternalToken(internalToken, expectedInternalToken);
        }
        return inboxService.deliver(request, subjectId);
    }
}
