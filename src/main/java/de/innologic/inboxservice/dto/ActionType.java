package de.innologic.inboxservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Typ einer UI-Aktion innerhalb einer Inbox-Nachricht.")
public enum ActionType {
    LINK
}
