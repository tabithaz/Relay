package dev.relay.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.relay.project.ProjectController;
import dev.relay.project.ProjectService;
import dev.relay.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
class WorkspaceAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockBean ProjectService projects;
    @MockBean WorkspacePolicy workspacePolicy;
    @MockBean JwtDecoder decoder;

    private void tokenFor(String subject) {
        when(decoder.decode("valid")).thenReturn(Jwt.withTokenValue("valid")
                .header("alg", "RS256").subject(subject).build());
    }

    @Test
    void anonymousCallerCannotListProjects() throws Exception {
        mvc.perform(get("/api/workspaces/1/projects"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(projects);
    }

    @Test
    void nonMemberCannotListProjects() throws Exception {
        tokenFor("outsider");
        mvc.perform(get("/api/workspaces/1/projects")
                .header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(projects);
    }

    @Test
    void viewerCanReadButCannotCreateProjects() throws Exception {
        tokenFor("viewer");
        when(workspacePolicy.canRead(1L, "viewer")).thenReturn(true);
        mvc.perform(get("/api/workspaces/1/projects")
                .header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/workspaces/1/projects")
                .header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Platform\",\"slug\":\"platform\"}"))
                .andExpect(status().isForbidden());
        verify(projects).list(1L, 0, 20);
        verify(projects, never()).create(any(), any(), any(), any());
    }

    @Test
    void editorCanCreateProjects() throws Exception {
        tokenFor("editor");
        when(workspacePolicy.canWrite(1L, "editor")).thenReturn(true);
        mvc.perform(post("/api/workspaces/1/projects")
                .header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Platform\",\"slug\":\"platform\"}"))
                .andExpect(status().isCreated());
        verify(projects).create(1L, "Platform", "platform", null);
    }
}
