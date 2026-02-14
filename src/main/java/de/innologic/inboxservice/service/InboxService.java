package de.innologic.inboxservice.service;

import de.innologic.inboxservice.dto.AttachmentRefDto;
import de.innologic.inboxservice.dto.InboxMessageDetailResponse;
import de.innologic.inboxservice.dto.InboxMessageListItemResponse;
import de.innologic.inboxservice.dto.InternalInboxDeliveryRequest;
import de.innologic.inboxservice.dto.InternalCreateInboxMessagesResponseDto;
import de.innologic.inboxservice.dto.InternalInboxMessageItem;
import de.innologic.inboxservice.dto.PageResponse;
import de.innologic.inboxservice.entity.InboxAttachmentRefEntity;
import de.innologic.inboxservice.entity.InboxMessageEntity;
import de.innologic.inboxservice.entity.InboxMessageStatus;
import de.innologic.inboxservice.exception.AccessDeniedException;
import de.innologic.inboxservice.exception.InboxMessageNotFoundException;
import de.innologic.inboxservice.exception.ValidationException;
import de.innologic.inboxservice.repository.InboxAttachmentRefRepository;
import de.innologic.inboxservice.repository.InboxMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InboxService implements InternalInboxDeliveryService, UserInboxMessageService {

    private final InboxMessageRepository inboxMessageRepository;
    private final InboxAttachmentRefRepository attachmentRefRepository;
    private final InboxMapper inboxMapper;

    public InboxService(InboxMessageRepository inboxMessageRepository,
                        InboxAttachmentRefRepository attachmentRefRepository,
                        InboxMapper inboxMapper) {
        this.inboxMessageRepository = inboxMessageRepository;
        this.attachmentRefRepository = attachmentRefRepository;
        this.inboxMapper = inboxMapper;
    }

    @Transactional
    @Override
    public InternalCreateInboxMessagesResponseDto deliver(InternalInboxDeliveryRequest request, String subjectIdHeader) {
        Instant now = Instant.now();
        List<String> messageIds = new ArrayList<>();
        String actorSubjectId = (subjectIdHeader == null || subjectIdHeader.isBlank()) ? request.sourceService() : subjectIdHeader;
        if (actorSubjectId == null || actorSubjectId.isBlank()) {
            throw new ValidationException("Actor information is required");
        }

        for (InternalInboxMessageItem messageItem : request.messages()) {
            String messageId = UUID.randomUUID().toString();
            InboxMessageEntity messageEntity = new InboxMessageEntity();
            messageEntity.setId(messageId);
            messageEntity.setCompanyId(request.companyId());
            messageEntity.setRecipientUserId(messageItem.recipientUserId());
            messageEntity.setTitle(messageItem.title());
            messageEntity.setBody(messageItem.body());
            messageEntity.setCategory(messageItem.category());
            messageEntity.setSeverity(messageItem.severity());
            messageEntity.setStatus(InboxMessageStatus.UNREAD);
            messageEntity.setSourceService(request.sourceService());
            messageEntity.setCorrelationId(request.correlationId());
            messageEntity.setExpiresAt(messageItem.expiresAt());
            messageEntity.setActionsJson(inboxMapper.toActionsJson(messageItem.actions()));
            messageEntity.setTagsJson(null);
            messageEntity.setExternalReference(null);
            messageEntity.setCreatedAt(now);
            messageEntity.setCreatedBy(actorSubjectId);
            messageEntity.setModifiedAt(now);
            messageEntity.setModifiedBy(actorSubjectId);

            inboxMessageRepository.save(messageEntity);
            messageIds.add(messageId);

            if (messageItem.attachments() != null) {
                for (AttachmentRefDto attachment : messageItem.attachments()) {
                    InboxAttachmentRefEntity attachmentEntity = new InboxAttachmentRefEntity();
                    attachmentEntity.setId(UUID.randomUUID().toString());
                    attachmentEntity.setCompanyId(request.companyId());
                    attachmentEntity.setMessageId(messageId);
                    attachmentEntity.setFileId(attachment.fileId());
                    attachmentEntity.setFilename(attachment.filename());
                    attachmentEntity.setMimeType(attachment.mimeType());
                    attachmentEntity.setSizeBytes(attachment.sizeBytes());
                    attachmentEntity.setCreatedAt(now);
                    attachmentRefRepository.save(attachmentEntity);
                }
            }
        }

        return new InternalCreateInboxMessagesResponseDto(messageIds.size(), messageIds);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<InboxMessageListItemResponse> listMessages(AuthContext authContext,
                                                                   boolean unreadOnly,
                                                                   String category,
                                                                   String severity,
                                                                   Instant from,
                                                                   Instant to,
                                                                   boolean includeTrashed,
                                                                   int page,
                                                                   int size,
                                                                   String sort) {
        Sort sortSpec = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortSpec);

        Specification<InboxMessageEntity> spec = Specification.where(byCompany(authContext.companyId()))
            .and(byRecipient(authContext.subjectId()));
        if (unreadOnly) {
            spec = spec.and(byStatus(InboxMessageStatus.UNREAD));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(byCategory(category));
        }
        if (severity != null && !severity.isBlank()) {
            spec = spec.and(bySeverity(severity));
        }
        if (from != null) {
            spec = spec.and(createdAtAfter(from));
        }
        if (to != null) {
            spec = spec.and(createdAtBefore(to));
        }
        if (!includeTrashed) {
            spec = spec.and(notTrashed());
        }

        Page<InboxMessageEntity> result = inboxMessageRepository.findAll(spec, pageRequest);
        List<InboxMessageListItemResponse> items = result.getContent().stream().map(inboxMapper::toSummaryDto).toList();
        return new PageResponse<>(result.getNumber(), result.getSize(), result.getTotalElements(), items);
    }

    @Transactional(readOnly = true)
    @Override
    public InboxMessageDetailResponse getMessage(AuthContext authContext, String messageId) {
        InboxMessageEntity entity = getAuthorizedMessage(authContext, messageId);
        List<InboxAttachmentRefEntity> attachments = attachmentRefRepository
            .findByCompanyIdAndMessageIdOrderByCreatedAtAsc(authContext.companyId(), messageId);
        return inboxMapper.toDetailDto(entity, attachments);
    }

    @Transactional
    @Override
    public InboxMessageDetailResponse markRead(AuthContext authContext, String messageId) {
        InboxMessageEntity entity = getAuthorizedMessage(authContext, messageId);
        Instant now = Instant.now();
        entity.setStatus(InboxMessageStatus.READ);
        entity.setReadAt(now);
        entity.setReadBy(authContext.subjectId());
        entity.setModifiedAt(now);
        entity.setModifiedBy(authContext.subjectId());
        inboxMessageRepository.save(entity);
        return getMessage(authContext, messageId);
    }

    @Transactional
    @Override
    public InboxMessageDetailResponse markUnread(AuthContext authContext, String messageId) {
        InboxMessageEntity entity = getAuthorizedMessage(authContext, messageId);
        Instant now = Instant.now();
        entity.setStatus(InboxMessageStatus.UNREAD);
        entity.setReadAt(null);
        entity.setReadBy(null);
        entity.setModifiedAt(now);
        entity.setModifiedBy(authContext.subjectId());
        inboxMessageRepository.save(entity);
        return getMessage(authContext, messageId);
    }

    @Transactional
    @Override
    public void softDelete(AuthContext authContext, String messageId) {
        InboxMessageEntity entity = getAuthorizedMessage(authContext, messageId);
        Instant now = Instant.now();
        entity.setTrashedAt(now);
        entity.setTrashedBy(authContext.subjectId());
        entity.setModifiedAt(now);
        entity.setModifiedBy(authContext.subjectId());
        inboxMessageRepository.save(entity);
    }

    @Transactional
    @Override
    public InboxMessageDetailResponse restore(AuthContext authContext, String messageId) {
        InboxMessageEntity entity = getAuthorizedMessage(authContext, messageId);
        Instant now = Instant.now();
        entity.setTrashedAt(null);
        entity.setTrashedBy(null);
        entity.setModifiedAt(now);
        entity.setModifiedBy(authContext.subjectId());
        inboxMessageRepository.save(entity);
        return getMessage(authContext, messageId);
    }

    private InboxMessageEntity getAuthorizedMessage(AuthContext authContext, String messageId) {
        InboxMessageEntity entity = inboxMessageRepository.findById(messageId)
            .orElseThrow(() -> new InboxMessageNotFoundException("Inbox message was not found"));

        if (!entity.getCompanyId().equals(authContext.companyId())) {
            throw new AccessDeniedException("Cross-company access denied");
        }
        if (!authContext.admin() && !entity.getRecipientUserId().equals(authContext.subjectId())) {
            throw new AccessDeniedException("Access to this message is denied");
        }
        return entity;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 ? Sort.Direction.fromString(parts[1].trim()) : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private Specification<InboxMessageEntity> byCompany(String companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    private Specification<InboxMessageEntity> byRecipient(String recipientUserId) {
        return (root, query, cb) -> cb.equal(root.get("recipientUserId"), recipientUserId);
    }

    private Specification<InboxMessageEntity> byStatus(InboxMessageStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<InboxMessageEntity> byCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private Specification<InboxMessageEntity> bySeverity(String severity) {
        return (root, query, cb) -> cb.equal(root.get("severity"), severity);
    }

    private Specification<InboxMessageEntity> createdAtAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private Specification<InboxMessageEntity> createdAtBefore(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    private Specification<InboxMessageEntity> notTrashed() {
        return (root, query, cb) -> cb.isNull(root.get("trashedAt"));
    }
}
