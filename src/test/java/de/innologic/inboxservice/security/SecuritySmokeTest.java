package de.innologic.inboxservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySmokeTest {

    private static final String COMPANY_ID = "01JCOMPANY123";
    private static final String USER_ID = "01JUSER123";
    private static final String INTERNAL_TOKEN = "change-me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void actuatorHealthAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void inboxMessagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/inbox/messages"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void inboxReadScopeAllowsListingMessages() throws Exception {
        mockMvc.perform(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .with(jwtWithScope("SCOPE_inbox.read", COMPANY_ID, USER_ID)))
            .andExpect(status().isOk());
    }

    @Test
    void inboxReadScopeOnlyRejectsWriteScope() throws Exception {
        mockMvc.perform(get("/inbox/messages")
                .header("X-Company-Id", "01JCOMPANY123")
                .header("X-Subject-Id", "01JUSER123")
                .with(jwtWithScope("SCOPE_inbox.write", COMPANY_ID, USER_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    void inboxReadRequiresTenantClaim() throws Exception {
        mockMvc.perform(get("/inbox/messages")
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .with(jwtWithScope("SCOPE_inbox.read", null, USER_ID)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));
    }

    @Test
    void internalDeliverScopeAllowsPostingMessages() throws Exception {
        mockMvc.perform(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalDeliveryPayload())
                .with(jwtWithScope("SCOPE_inbox.deliver", COMPANY_ID, "messaging-service")))
            .andExpect(status().isOk());
    }

    @Test
    void internalDeliverRequiresDeliverScope() throws Exception {
        mockMvc.perform(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalDeliveryPayload())
                .header("X-Internal-Token", "change-me")
                .with(jwtWithScope("SCOPE_inbox.read", COMPANY_ID, USER_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    void internalDeliverLegacyTokenWithoutJwtIsRejectedByDefault() throws Exception {
        mockMvc.perform(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalDeliveryPayload())
                .header("X-Internal-Token", INTERNAL_TOKEN))
            .andExpect(status().isForbidden());
    }

    @Test
    void tenantMatchingJwtAllowsMessageAccess() throws Exception {
        String messageId = deliverSingleMessage();

        mockMvc.perform(get("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .with(jwtWithScope("SCOPE_inbox.read", COMPANY_ID, USER_ID)))
            .andExpect(status().isOk());
    }

    @Test
    void tenantMismatchJwtReturnsTenantMismatchCode() throws Exception {
        String messageId = deliverSingleMessage();

        mockMvc.perform(get("/inbox/messages/{id}", messageId)
                .header("X-Company-Id", COMPANY_ID)
                .header("X-Subject-Id", USER_ID)
                .with(jwtWithScope("SCOPE_inbox.read", "01JOTHERCOMPANY", USER_ID)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_MISMATCH"));
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

    private String internalDeliveryPayload() {
        return """
            {
              "companyId":"01JCOMPANY123",
              "sourceService":"messaging-service",
              "messages":[
                {
                  "recipientUserId":"01JUSER123",
                  "title":"Hello",
                  "body":"content",
                  "category":"TRANSACTIONAL",
                  "severity":"INFO",
                  "expiresAt":"2026-05-01T00:00:00Z"
                }
              ]
            }
            """;
    }

    private String deliverSingleMessage() throws Exception {
        String response = mockMvc.perform(post("/internal/inbox/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(internalDeliveryPayload())
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .with(jwtWithScope("SCOPE_inbox.deliver", COMPANY_ID, "messaging-service")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("messageIds").get(0).asText();
    }
}
