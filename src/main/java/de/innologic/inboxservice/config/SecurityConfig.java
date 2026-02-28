package de.innologic.inboxservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ACTUATOR_ENDPOINTS = {
        "/actuator/health",
        "/actuator/info",
        "/api/v1/actuator/health",
        "/api/v1/actuator/info"
    };

    private final boolean legacyInternalTokenEnabled;
    private final String legacyInternalToken;

    public SecurityConfig(@Value("${inbox.security.legacy-internal-token-enabled:false}") boolean legacyInternalTokenEnabled,
                          @Value("${inbox.security.internal-token}") String legacyInternalToken) {
        this.legacyInternalTokenEnabled = legacyInternalTokenEnabled;
        this.legacyInternalToken = legacyInternalToken;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(PUBLIC_ACTUATOR_ENDPOINTS).permitAll()
                    .requestMatchers(HttpMethod.GET, "/inbox/**").hasAuthority("SCOPE_inbox.read")
                    .requestMatchers(HttpMethod.POST, "/inbox/**").hasAuthority("SCOPE_inbox.write")
                    .requestMatchers(HttpMethod.DELETE, "/inbox/**").hasAuthority("SCOPE_inbox.write")
                    .requestMatchers(HttpMethod.POST, "/internal/inbox/messages").access(
                        new InternalDeliveryAuthorizationManager(legacyInternalTokenEnabled, legacyInternalToken))
                    .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    private static final class InternalDeliveryAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

        private static final SimpleGrantedAuthority DELIVER_AUTHORITY = new SimpleGrantedAuthority("SCOPE_inbox.deliver");

        private final boolean legacyEnabled;
        private final String legacyToken;

        private InternalDeliveryAuthorizationManager(boolean legacyEnabled, String legacyToken) {
            this.legacyEnabled = legacyEnabled;
            this.legacyToken = legacyToken;
        }

        @Override
        public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
            HttpServletRequest request = context.getRequest();
            if (legacyEnabled && matchesLegacyToken(request)) {
                return new AuthorizationDecision(true);
            }
            Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            boolean hasScope = auth.getAuthorities().stream()
                .anyMatch(authority -> DELIVER_AUTHORITY.getAuthority().equals(authority.getAuthority()));
            return new AuthorizationDecision(hasScope);
        }

        private boolean matchesLegacyToken(HttpServletRequest request) {
            if (legacyToken == null || legacyToken.isBlank()) {
                return false;
            }
            String header = request.getHeader("X-Internal-Token");
            return header != null && header.equals(legacyToken);
        }
    }
}
