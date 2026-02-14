package de.innologic.inboxservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.inboxservice.dto.ActionDto;
import de.innologic.inboxservice.dto.AttachmentRefDto;
import de.innologic.inboxservice.dto.InboxMessageDetailResponse;
import de.innologic.inboxservice.dto.InboxMessageListItemResponse;
import de.innologic.inboxservice.entity.InboxAttachmentRefEntity;
import de.innologic.inboxservice.entity.InboxMessageEntity;
import de.innologic.inboxservice.exception.ErrorCode;
import de.innologic.inboxservice.exception.InboxServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class InboxMapper {

    private final ObjectMapper objectMapper;

    public InboxMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toActionsJson(List<ActionDto> actions) {
        return writeJson(actions);
    }

    public String toTagsJson(List<String> tags) {
        return writeJson(tags);
    }

    public List<ActionDto> fromActionsJson(String actionsJson) {
        if (actionsJson == null || actionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(actionsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new InboxServiceException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNEXPECTED_ERROR, "Failed to parse actions JSON");
        }
    }

    public List<String> fromTagsJson(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new InboxServiceException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNEXPECTED_ERROR, "Failed to parse tags JSON");
        }
    }

    public InboxMessageListItemResponse toSummaryDto(InboxMessageEntity entity) {
        return new InboxMessageListItemResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getCategory(),
            entity.getSeverity(),
            entity.getStatus().name(),
            entity.getCreatedAt()
        );
    }

    public InboxMessageDetailResponse toDetailDto(InboxMessageEntity entity, List<InboxAttachmentRefEntity> attachments) {
        return new InboxMessageDetailResponse(
            entity.getId(),
            entity.getCompanyId(),
            entity.getRecipientUserId(),
            entity.getTitle(),
            entity.getBody(),
            entity.getCategory(),
            entity.getSeverity(),
            entity.getStatus().name(),
            entity.getSourceService(),
            entity.getCorrelationId(),
            entity.getExpiresAt(),
            entity.getReadAt(),
            entity.getReadBy(),
            fromActionsJson(entity.getActionsJson()),
            attachments.stream().map(this::toAttachmentDto).toList(),
            entity.getCreatedAt(),
            entity.getCreatedBy(),
            entity.getModifiedAt(),
            entity.getModifiedBy(),
            entity.getTrashedAt(),
            entity.getTrashedBy(),
            entity.getVersion()
        );
    }

    private AttachmentRefDto toAttachmentDto(InboxAttachmentRefEntity entity) {
        return new AttachmentRefDto(
            entity.getFileId(),
            entity.getFilename(),
            entity.getMimeType(),
            entity.getSizeBytes()
        );
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InboxServiceException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNEXPECTED_ERROR, "Failed to serialize JSON payload");
        }
    }
}
