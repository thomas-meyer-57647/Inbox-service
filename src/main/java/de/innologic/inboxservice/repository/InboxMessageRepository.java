package de.innologic.inboxservice.repository;

import de.innologic.inboxservice.entity.InboxMessageEntity;
import de.innologic.inboxservice.entity.InboxMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InboxMessageRepository extends JpaRepository<InboxMessageEntity, String>, JpaSpecificationExecutor<InboxMessageEntity> {
    Page<InboxMessageEntity> findByCompanyIdAndRecipientUserIdAndTrashedAtIsNullOrderByCreatedAtDesc(
        String companyId,
        String recipientUserId,
        Pageable pageable
    );

    Page<InboxMessageEntity> findByCompanyIdAndRecipientUserIdAndStatusAndTrashedAtIsNullOrderByCreatedAtDesc(
        String companyId,
        String recipientUserId,
        InboxMessageStatus status,
        Pageable pageable
    );

    Page<InboxMessageEntity> findByCompanyIdAndRecipientUserIdOrderByCreatedAtDesc(
        String companyId,
        String recipientUserId,
        Pageable pageable
    );

    Page<InboxMessageEntity> findByCompanyIdAndRecipientUserIdAndStatusOrderByCreatedAtDesc(
        String companyId,
        String recipientUserId,
        InboxMessageStatus status,
        Pageable pageable
    );
}
