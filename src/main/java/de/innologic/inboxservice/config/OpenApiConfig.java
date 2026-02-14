package de.innologic.inboxservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Inbox Service API",
        version = "1.0",
        description = "REST API fuer IN_APP / Inbox Nachrichten im Multi-Mandanten-Kontext.",
        contact = @Contact(name = "Inbox Service Team")
    ),
    servers = {
        @Server(url = "/api/v1", description = "Default API context path")
    }
)
public class OpenApiConfig {
}
