package dev.relay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.relay.workspace.WorkspaceController;
import dev.relay.workspace.WorkspaceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockBean WorkspaceService workspaces;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void missingBearerTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/workspaces")).andExpect(status().isUnauthorized());
        verifyNoInteractions(workspaces);
    }

    @Test
    void invalidBearerTokenIsUnauthorized() throws Exception {
        when(jwtDecoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));
        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(workspaces);
    }

    @Test
    void validBearerTokenCanReachWorkspaceApi() throws Exception {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "RS256")
                .subject("user-42").build();
        when(jwtDecoder.decode("valid")).thenReturn(jwt);
        when(workspaces.list(0, 20)).thenReturn(Page.empty());

        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
        verify(workspaces).list(0, 20);
    }

    @Test
    void requiredAudienceMustBePresent() {
        Jwt valid = Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("aud", List.of("another-api", "relay-api")).build();
        Jwt invalid = Jwt.withTokenValue("token").header("alg", "RS256")
                .claim("aud", List.of("another-api")).build();
        Jwt missing = Jwt.withTokenValue("token").header("alg", "RS256").build();

        assertThat(SecurityConfig.audienceValidator("relay-api").validate(valid).hasErrors()).isFalse();
        assertThat(SecurityConfig.audienceValidator("relay-api").validate(invalid).hasErrors()).isTrue();
        assertThat(SecurityConfig.audienceValidator("relay-api").validate(missing).hasErrors()).isTrue();
    }
}
