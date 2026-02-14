package de.innologic.inboxservice.service;

import de.innologic.inboxservice.dto.InternalCreateInboxMessagesResponseDto;
import de.innologic.inboxservice.dto.InternalInboxDeliveryRequest;

public interface InternalInboxDeliveryService {
    InternalCreateInboxMessagesResponseDto deliver(InternalInboxDeliveryRequest request, String subjectIdHeader);
}
