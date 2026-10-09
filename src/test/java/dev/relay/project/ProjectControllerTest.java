package dev.relay.project;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.relay.workspace.WorkspaceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ProjectService service;

    @Test
    void createsProject() throws Exception {
        mvc.perform(post("/api/workspaces/1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Platform\",\"slug\":\"platform\",\"description\":\"Roadmap\"}"))
                .andExpect(status().isCreated());
        verify(service).create(1L, "Platform", "platform", "Roadmap");
    }

    @Test
    void rejectsBlankName() throws Exception {
        mvc.perform(post("/api/workspaces/1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \",\"slug\":\"platform\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidSlug() throws Exception {
        mvc.perform(post("/api/workspaces/1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Platform\",\"slug\":\"Bad--Slug\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsOversizedDescription() throws Exception {
        String payload = "{\"name\":\"Platform\",\"slug\":\"platform\",\"description\":\""
                + "x".repeat(2001) + "\"}";
        mvc.perform(post("/api/workspaces/1/projects")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBlankUpdate() throws Exception {
        mvc.perform(patch("/api/workspaces/1/projects/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForProjectOutsideWorkspace() throws Exception {
        when(service.get(2L, 7L)).thenThrow(new ProjectNotFoundException());

        mvc.perform(get("/api/workspaces/2/projects/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForMissingWorkspace() throws Exception {
        when(service.list(404L, 0, 20)).thenThrow(new WorkspaceNotFoundException());

        mvc.perform(get("/api/workspaces/404/projects"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictForDuplicateProjectSlug() throws Exception {
        when(service.create(1L, "Platform", "platform", null))
                .thenThrow(new ProjectSlugConflictException());

        mvc.perform(post("/api/workspaces/1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Platform\",\"slug\":\"platform\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsOversizedPageSize() throws Exception {
        mvc.perform(get("/api/workspaces/1/projects?size=101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
