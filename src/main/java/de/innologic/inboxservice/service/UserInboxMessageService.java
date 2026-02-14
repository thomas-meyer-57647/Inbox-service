package de.innologic.inboxservice.service;

import de.innologic.inboxservice.dto.InboxMessageDetailResponse;
import de.innologic.inboxservice.dto.InboxMessageListItemResponse;
import de.innologic.inboxservice.dto.PageResponse;

import java.time.Instant;

public interface UserInboxMessageService {
    PageResponse<InboxMessageListItemResponse> listMessages(AuthContext authContext,
                                                            boolean unreadOnly,
                                                            String category,
                                                            String severity,
                                                            Instant from,
                                                            Instant to,
                                                            boolean includeTrashed,
                                                            int page,
                                                            int size,
                                                            String sort);

    InboxMessageDetailResponse getMessage(AuthContext authContext, String messageId);

    InboxMessageDetailResponse markRead(AuthContext authContext, String messageId);

    InboxMessageDetailResponse markUnread(AuthContext authContext, String messageId);

    void softDelete(AuthContext authContext, String messageId);

    InboxMessageDetailResponse restore(AuthContext authContext, String messageId);
}
