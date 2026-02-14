package de.innologic.inboxservice.controller;

import de.innologic.inboxservice.dto.ErrorResponse;
import de.innologic.inboxservice.dto.InboxMessageDetailResponse;
import de.innologic.inboxservice.dto.InboxMessageListItemResponse;
import de.innologic.inboxservice.dto.PageResponse;
import de.innologic.inboxservice.service.AuthContext;
import de.innologic.inboxservice.service.AuthContextResolver;
import de.innologic.inboxservice.service.UserInboxMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Validated
@RequestMapping("/inbox/messages")
@Tag(name = "User Inbox", description = "Inbox-Endpunkte fuer Benutzer und Admins")
public class UserInboxController {

    private final UserInboxMessageService inboxService;
    private final AuthContextResolver authContextResolver;

    public UserInboxController(UserInboxMessageService inboxService, AuthContextResolver authContextResolver) {
        this.inboxService = inboxService;
        this.authContextResolver = authContextResolver;
    }

    @Operation(
        summary = "Inbox-Nachrichten listen",
        description = "Liefert paginierte Inbox-Nachrichten fuer den aktuellen Benutzer. " +
            "Standardmaessig werden nur nicht geloeschte Nachrichten geliefert."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste erfolgreich geladen", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = PageResponse.class),
            examples = @ExampleObject(
                name = "pagedList",
                summary = "Eine Seite mit einer ungelesenen Nachricht",
                value = "{\"page\":0,\"size\":50,\"total\":1,\"items\":[{\"messageId\":\"01JMSG123\",\"title\":\"Ticket aktualisiert\",\"category\":\"TRANSACTIONAL\",\"severity\":\"INFO\",\"status\":\"UNREAD\",\"createdAt\":\"2026-02-14T09:00:00Z\"}]}"
            )
        )),
        @ApiResponse(responseCode = "400", description = "Ungueltige Parameter", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "invalidPaging",
                summary = "Ungueltiger Paging-Parameter",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":400,\"errorCode\":\"INVALID_REQUEST\",\"message\":\"size must be >= 1\",\"path\":\"/api/v1/inbox/messages\",\"correlationId\":\"c-123\",\"details\":[\"size must be greater than 0\"]}"
            )
        )),
        @ApiResponse(responseCode = "403", description = "Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "forbidden",
                summary = "Fehlende Berechtigung",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":403,\"errorCode\":\"ACCESS_DENIED\",\"message\":\"Access denied\",\"path\":\"/api/v1/inbox/messages\",\"correlationId\":\"c-123\",\"details\":[]}"
            )
        )),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        ))
    })
    @GetMapping
    public PageResponse<InboxMessageListItemResponse> listMessages(
        @Parameter(description = "Mandanten-ID aus Auth-Kontext", required = true, example = "01JCOMPANY123")
        @RequestHeader(AuthContextResolver.HEADER_COMPANY_ID) String companyId,
        @Parameter(description = "Benutzer-ID aus Auth-Kontext", required = true, example = "01JUSER123")
        @RequestHeader(AuthContextResolver.HEADER_SUBJECT_ID) String subjectId,
        @Parameter(description = "Rolle des Benutzers (USER oder ADMIN)", required = false, example = "USER")
        @RequestHeader(value = AuthContextResolver.HEADER_ROLE, required = false) String role,
        @Parameter(description = "Nur ungelesene Nachrichten liefern", example = "false")
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @Parameter(description = "Filter nach Kategorie", example = "TRANSACTIONAL")
        @RequestParam(required = false) String category,
        @Parameter(description = "Filter nach Severity", example = "INFO")
        @RequestParam(required = false) String severity,
        @Parameter(description = "Filter von Zeit (createdAt >= from, UTC ISO-8601)", example = "2026-02-01T00:00:00Z")
        @RequestParam(required = false) Instant from,
        @Parameter(description = "Filter bis Zeit (createdAt <= to, UTC ISO-8601)", example = "2026-02-28T23:59:59Z")
        @RequestParam(required = false) Instant to,
        @Parameter(description = "Papierkorb-Nachrichten einschliessen", example = "false")
        @RequestParam(defaultValue = "false") boolean includeTrashed,
        @Parameter(description = "Seitennummer (0-basiert)", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Seitengroesse (max 200)", example = "50")
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
        @Parameter(description = "Sortierung im Format feld,Richtung", example = "createdAt,DESC")
        @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        AuthContext authContext = authContextResolver.resolveUserContext(companyId, subjectId, role);
        return inboxService.listMessages(authContext, unreadOnly, category, severity, from, to, includeTrashed, page, size, sort);
    }

    @Operation(summary = "Inbox-Nachricht im Detail laden", description = "Liefert alle Felder einer Nachricht inkl. Attachment-Referenzen.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nachricht gefunden", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = InboxMessageDetailResponse.class)
        )),
        @ApiResponse(responseCode = "400", description = "Ungueltiger Request", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "404", description = "Nachricht nicht gefunden", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(
                name = "notFound",
                summary = "Message-ID existiert nicht",
                value = "{\"timestamp\":\"2026-02-14T12:00:00Z\",\"status\":404,\"errorCode\":\"INBOX_MESSAGE_NOT_FOUND\",\"message\":\"Inbox message was not found\",\"path\":\"/api/v1/inbox/messages/01JMSG123\",\"correlationId\":\"c-123\",\"details\":[]}"
            )
        )),
        @ApiResponse(responseCode = "403", description = "Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        ))
    })
    @GetMapping("/{id}")
    public InboxMessageDetailResponse getMessage(
        @Parameter(description = "Mandanten-ID aus Auth-Kontext", required = true, example = "01JCOMPANY123")
        @RequestHeader(AuthContextResolver.HEADER_COMPANY_ID) String companyId,
        @Parameter(description = "Benutzer-ID aus Auth-Kontext", required = true, example = "01JUSER123")
        @RequestHeader(AuthContextResolver.HEADER_SUBJECT_ID) String subjectId,
        @Parameter(description = "Rolle des Benutzers (USER oder ADMIN)", required = false, example = "USER")
        @RequestHeader(value = AuthContextResolver.HEADER_ROLE, required = false) String role,
        @Parameter(description = "Message-ID", required = true, example = "01JMSG123")
        @PathVariable("id") String id
    ) {
        AuthContext authContext = authContextResolver.resolveUserContext(companyId, subjectId, role);
        return inboxService.getMessage(authContext, id);
    }

    @Operation(summary = "Nachricht als READ markieren", description = "Setzt status=READ sowie readAt/readBy.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nachricht wurde als gelesen markiert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = InboxMessageDetailResponse.class)
        )),
        @ApiResponse(responseCode = "404", description = "Nachricht nicht gefunden", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "403", description = "Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "409", description = "Optimistic-Lock-Konflikt", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        ))
    })
    @PostMapping("/{id}/read")
    public InboxMessageDetailResponse markRead(
        @Parameter(description = "Mandanten-ID aus Auth-Kontext", required = true, example = "01JCOMPANY123")
        @RequestHeader(AuthContextResolver.HEADER_COMPANY_ID) String companyId,
        @Parameter(description = "Benutzer-ID aus Auth-Kontext", required = true, example = "01JUSER123")
        @RequestHeader(AuthContextResolver.HEADER_SUBJECT_ID) String subjectId,
        @Parameter(description = "Rolle des Benutzers (USER oder ADMIN)", required = false, example = "USER")
        @RequestHeader(value = AuthContextResolver.HEADER_ROLE, required = false) String role,
        @Parameter(description = "Message-ID", required = true, example = "01JMSG123")
        @PathVariable("id") String id
    ) {
        AuthContext authContext = authContextResolver.resolveUserContext(companyId, subjectId, role);
        return inboxService.markRead(authContext, id);
    }

    @Operation(summary = "Nachricht als UNREAD markieren", description = "Setzt status=UNREAD und entfernt readAt/readBy.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nachricht wurde als ungelesen markiert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = InboxMessageDetailResponse.class)
        )),
        @ApiResponse(responseCode = "404", description = "Nachricht nicht gefunden", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "403", description = "Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "409", description = "Optimistic-Lock-Konflikt", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        ))
    })
    @PostMapping("/{id}/unread")
    public InboxMessageDetailResponse markUnread(
        @Parameter(description = "Mandanten-ID aus Auth-Kontext", required = true, example = "01JCOMPANY123")
        @RequestHeader(AuthContextResolver.HEADER_COMPANY_ID) String companyId,
        @Parameter(description = "Benutzer-ID aus Auth-Kontext", required = true, example = "01JUSER123")
        @RequestHeader(AuthContextResolver.HEADER_SUBJECT_ID) String subjectId,
        @Parameter(description = "Rolle des Benutzers (USER oder ADMIN)", required = false, example = "USER")
        @RequestHeader(value = AuthContextResolver.HEADER_ROLE, required = false) String role,
        @Parameter(description = "Message-ID", required = true, example = "01JMSG123")
        @PathVariable("id") String id
    ) {
        AuthContext authContext = authContextResolver.resolveUserContext(companyId, subjectId, role);
        return inboxService.markUnread(authContext, id);
    }

    @Operation(summary = "Nachricht soft-loeschen", description = "Verschiebt eine Nachricht in den Papierkorb (trashedAt/trashedBy).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nachricht in Papierkorb verschoben"),
        @ApiResponse(responseCode = "400", description = "Ungueltiger Request", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "404", description = "Nachricht nicht gefunden", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "403", description = "Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "409", description = "Optimistic-Lock-Konflikt", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "500", description = "Unerwarteter Fehler", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        ))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
        @Parameter(description = "Mandanten-ID aus Auth-Kontext", required = true, example = "01JCOMPANY123")
        @RequestHeader(AuthContextResolver.HEADER_COMPANY_ID) String companyId,
        @Parameter(description = "Benutzer-ID aus Auth-Kontext", required = true, example = "01JUSER123")
        @RequestHeader(AuthContextResolver.HEADER_SUBJECT_ID) String subjectId,
        @Parameter(description = "Rolle des Benutzers (USER oder ADMIN)", required = false, example = "USER")
        @RequestHeader(value = AuthContextResolver.HEADER_ROLE, required = false) String role,
        @Parameter(description = "Message-ID", required = true, example = "01JMSG123")
        @PathVariable("id") String id
    ) {
        AuthContext authContext = authContextResolver.resolveUserContext(companyId, subjectId, role);
        inboxService.softDelete(authContext, id);
    }

    @Operation(summary = "Nachricht aus Papierkorb wiederherstellen", description = "Setzt trashedAt/trashedBy zurueck und aktiviert die Nachricht wieder.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nachricht wiederhergestellt", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = InboxMessageDetailResponse.class)
        )),
        @ApiResponse(responseCode = "404", description = "Nachricht nicht gefunden", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )),
        @ApiResponse(responseCode = "403", description = "Zugriff verweigert", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        ))
    })
    @PostMapping("/{messageId}/restore")
    public InboxMessageDetailResponse restore(
        @Parameter(description = "Mandanten-ID aus Auth-Kontext", required = true, example = "01JCOMPANY123")
        @RequestHeader(AuthContextResolver.HEADER_COMPANY_ID) String companyId,
        @Parameter(description = "Benutzer-ID aus Auth-Kontext", required = true, example = "01JUSER123")
        @RequestHeader(AuthContextResolver.HEADER_SUBJECT_ID) String subjectId,
        @Parameter(description = "Rolle des Benutzers (USER oder ADMIN)", required = false, example = "USER")
        @RequestHeader(value = AuthContextResolver.HEADER_ROLE, required = false) String role,
        @Parameter(description = "Message-ID", required = true, example = "01JMSG123")
        @PathVariable String messageId
    ) {
        AuthContext authContext = authContextResolver.resolveUserContext(companyId, subjectId, role);
        return inboxService.restore(authContext, messageId);
    }
}
