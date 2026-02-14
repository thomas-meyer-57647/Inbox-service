package de.innologic.inboxservice.repository;

import de.innologic.inboxservice.entity.InboxAttachmentRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InboxAttachmentRefRepository extends JpaRepository<InboxAttachmentRefEntity, String> {
    List<InboxAttachmentRefEntity> findByCompanyIdAndMessageIdOrderByCreatedAtAsc(String companyId, String messageId);
}
