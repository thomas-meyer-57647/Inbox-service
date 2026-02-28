package de.innologic.inboxservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.inboxservice.entity.InboxMessageEntity;
import de.innologic.inboxservice.repository.InboxAttachmentRefRepository;
import de.innologic.inboxservice.repository.InboxMessageRepository;
import de.innologic.inboxservice.service.InboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InboxControllerIntegrationTest {

    private static final String INTERNAL_TOKEN = "change-me";
    private static final String COMPANY_ID = "01JCOMPANY123";
    private static final String USER_ID = "01JUSER123";
    private static final String OTHER_USER_ID = "01JUSER999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboxMessageRepository inboxMessageRepository;

    @Autowired
    private InboxAttachmentRefRepository inboxAttachmentRefRepository;

    @MockitoSpyBean
    private InboxService inboxService;

    @BeforeEach
    void cleanDbBefore() {
        inboxAttachmentRefRepository.deleteAll();
        inboxMessageRepository.deleteAll();
    }

    @AfterEach
    void resetSpy() {
        reset(inboxService);
    }

    @Test
    void internalDeliveryCreatesMessagesSuccessfully_batch() throws Exception {
        performDeliver(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .content(internalDeliveryPayload(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.createdCount").value(2))
            .andExpect(jsonPath("$.messageIds.length()").value(2));

        assertThat(inboxMessageRepository.count()).isEqualTo(2);
    }

    @Test
    void userListReturnsPagingCorrectly() throws Exception {
        deliverBatch(3);

        performRead(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void adminListReturnsTenantWideMessages() throws Exception {
        deliverSingleAndGetMessageId();
        deliverSingleMessageFor(OTHER_USER_ID);

        mockMvc.perform(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .header("X-Role", "ADMIN")
                .with(jwtWithScope("SCOPE_inbox.read", COMPANY_ID, USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void nonAdminListRemainsSubjectScoped() throws Exception {
        deliverSingleAndGetMessageId();
        deliverSingleMessageFor(OTHER_USER_ID);

        performRead(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void detailReturnsMessage() throws Exception {
        String messageId = deliverSingleAndGetMessageId();

        performRead(get("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value(messageId))
            .andExpect(jsonPath("$.companyId").value(COMPANY_ID))
            .andExpect(jsonPath("$.recipientUserId").value(USER_ID))
            .andExpect(jsonPath("$.attachments.length()").value(1))
            .andExpect(jsonPath("$.actions.length()").value(1));
    }

    @Test
    void markReadAndUnreadChangesStatusCorrectly() throws Exception {
        String messageId = deliverSingleAndGetMessageId();

        performWrite(post("/inbox/messages/{id}/read", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READ"))
            .andExpect(jsonPath("$.readAt").isNotEmpty())
            .andExpect(jsonPath("$.readBy").value(USER_ID));

        performWrite(post("/inbox/messages/{id}/unread", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNREAD"))
            .andExpect(jsonPath("$.readAt").isEmpty())
            .andExpect(jsonPath("$.readBy").isEmpty());
    }

    @Test
    void softDeleteSetsTrashedAtAndMessageDisappearsFromDefaultList() throws Exception {
        String messageId = deliverSingleAndGetMessageId();

        performWrite(delete("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isNoContent());

        InboxMessageEntity entity = inboxMessageRepository.findById(messageId).orElseThrow();
        assertThat(entity.getTrashedAt()).isNotNull();
        assertThat(entity.getTrashedBy()).isEqualTo(USER_ID);

        performRead(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void validationMissingMandatoryFieldsReturns400WithDetails() throws Exception {
        String invalidPayload = """
            {
              "companyId": "",
              "sourceService": "",
              "messages": []
            }
            """;

        performDeliver(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .content(invalidPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.details.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void accessDeniedForWrongCompanyOrWrongSubjectWithoutAdmin() throws Exception {
        String messageId = deliverSingleAndGetMessageId();

        performRead(get("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", "01JOTHERCOMPANY")
                .header("X-Subject-Id", USER_ID),
            "01JOTHERCOMPANY", USER_ID)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_MISMATCH"));

        performRead(get("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", OTHER_USER_ID),
            COMPANY_ID, OTHER_USER_ID)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void unknownMessageIdReturns404() throws Exception {
        performRead(get("/inbox/messages/{id}", UUID.randomUUID())
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("INBOX_MESSAGE_NOT_FOUND"));
    }

    @Test
    void optimisticLockConflictReturns409() throws Exception {
        String messageId = UUID.randomUUID().toString();
        doThrow(new ObjectOptimisticLockingFailureException(InboxMessageEntity.class, messageId))
            .when(inboxService)
            .markRead(any(), eq(messageId));

        performWrite(post("/inbox/messages/{id}/read", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("OPTIMISTIC_LOCK_FAILED"));
    }

    @Test
    void includeTrashedFalseDoesNotReturnDeletedMessages() throws Exception {
        String messageId = deliverSingleAndGetMessageId();

        performWrite(delete("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID))
            .andExpect(status().isNoContent());

        performRead(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .param("includeTrashed", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));
    }

    private void deliverBatch(int count) throws Exception {
        performDeliver(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .content(internalDeliveryPayload(count)))
            .andExpect(status().isOk());
    }

    private String deliverSingleAndGetMessageId() throws Exception {
        String response = performDeliver(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .content(internalDeliveryPayload(1)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("messageIds").get(0).asText();
    }

    private String internalDeliveryPayload(int count) {
        StringBuilder messages = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                messages.append(",");
            }
            messages.append("""
                {
                  "recipientUserId":"01JUSER123",
                  "title":"Ticket aktualisiert %d",
                  "body":"Dein Ticket wurde bearbeitet.",
                  "category":"TRANSACTIONAL",
                  "severity":"INFO",
                  "expiresAt":"2026-05-01T00:00:00Z",
                  "actions":[{"label":"Ticket oeffnen","url":"/tickets/4711","actionType":"LINK"}],
                  "attachments":[{"fileId":"01JFILE123","filename":"report.pdf","mimeType":"application/pdf","sizeBytes":32768}]
                }
                """.formatted(i + 1));
        }
        return """
            {
              "companyId":"01JCOMPANY123",
              "sourceService":"messaging-service",
              "correlationId":"c-123",
              "messages":[%s]
            }
            """.formatted(messages);
    }

    private ResultActions performRead(MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder.with(jwtWithScope("SCOPE_inbox.read", COMPANY_ID, USER_ID)));
    }

    private ResultActions performRead(MockHttpServletRequestBuilder builder, String tenantId, String subject) throws Exception {
        return mockMvc.perform(builder.with(jwtWithScope("SCOPE_inbox.read", tenantId, subject)));
    }

    private ResultActions performWrite(MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder.with(jwtWithScope("SCOPE_inbox.write", COMPANY_ID, USER_ID)));
    }

    private ResultActions performDeliver(MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder.with(jwtWithScope("SCOPE_inbox.deliver", COMPANY_ID, "messaging-service")));
    }

    private void deliverSingleMessageFor(String recipientUserId) throws Exception {
        mockMvc.perform(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalDeliveryPayloadForRecipient(recipientUserId))
                .with(jwtWithScope("SCOPE_inbox.deliver", COMPANY_ID, "messaging-service")))
            .andExpect(status().isOk());
    }

    private String internalDeliveryPayloadForRecipient(String recipientUserId) {
        return """
            {
              "companyId":"01JCOMPANY123",
              "sourceService":"messaging-service",
              "correlationId":"c-123",
              "messages":[
                {
                  "recipientUserId":"%s",
                  "title":"Ticket aktualisiert",
                  "body":"Dein Ticket wurde bearbeitet.",
                  "category":"TRANSACTIONAL",
                  "severity":"INFO",
                  "expiresAt":"2026-05-01T00:00:00Z",
                  "actions":[{"label":"Ticket oeffnen","url":"/tickets/4711","actionType":"LINK"}],
                  "attachments":[{"fileId":"01JFILE123","filename":"report.pdf","mimeType":"application/pdf","sizeBytes":32768}]
                }
              ]
            }
            """.formatted(recipientUserId);
    }

    private RequestPostProcessor jwtWithScope(String scope, String tenantId, String subject) {
        return jwt()
            .authorities(new SimpleGrantedAuthority(scope))
            .jwt(builder -> {
                if (tenantId != null) {
                    builder.claim("tenant_id", tenantId);
                }
                if (subject != null) {
                    builder.subject(subject);
                }
            });
    }
}
