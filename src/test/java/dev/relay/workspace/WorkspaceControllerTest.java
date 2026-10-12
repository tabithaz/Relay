package dev.relay.workspace;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {
    @Autowired MockMvc mvc;
    @MockBean WorkspaceService service;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void createsWorkspace() throws Exception {
        when(service.create("Engineering", "engineering", "user-42"))
                .thenReturn(WorkspaceResponse.from(new Workspace("Engineering", "engineering")));

        mvc.perform(post("/api/workspaces").principal(() -> "user-42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Engineering\",\"slug\":\"engineering\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.slug").value("engineering"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mvc.perform(post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \",\"slug\":\"engineering\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidSlug() throws Exception {
        mvc.perform(post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Engineering\",\"slug\":\"Bad--Slug\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingSlug() throws Exception {
        mvc.perform(post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBlankRename() throws Exception {
        mvc.perform(patch("/api/workspaces/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsConflictForExistingSlug() throws Exception {
        when(service.create("Engineering", "engineering", "user-42"))
                .thenThrow(new WorkspaceSlugConflictException());

        mvc.perform(post("/api/workspaces").principal(() -> "user-42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Engineering\",\"slug\":\"engineering\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundForMissingWorkspace() throws Exception {
        when(service.get(999L)).thenThrow(new WorkspaceNotFoundException());

        mvc.perform(get("/api/workspaces/999"))
                .andExpect(status().isNotFound());
    }
}
