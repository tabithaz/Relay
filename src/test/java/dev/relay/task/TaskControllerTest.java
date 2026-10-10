package dev.relay.task;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.relay.project.ProjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {
    private static final String PATH = "/api/workspaces/1/projects/7/tasks";

    @Autowired MockMvc mvc;
    @MockBean TaskService service;

    @Test
    void createsTask() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Fix login\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated());
        verify(service).create(1L, 7L, "Fix login", null, TaskPriority.HIGH, null);
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  \",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingPriority() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Fix login\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidDueDate() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Fix login\",\"priority\":\"HIGH\",\"dueAt\":\"tomorrow\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsOversizedDescription() throws Exception {
        String body = "{\"title\":\"Fix login\",\"priority\":\"HIGH\",\"description\":\""
                + "x".repeat(4001) + "\"}";
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void filtersByStatus() throws Exception {
        mvc.perform(get(PATH + "?status=BLOCKED&page=2&size=10"))
                .andExpect(status().isOk());
        verify(service).list(1L, 7L, TaskStatus.BLOCKED, 2, 10);
    }

    @Test
    void rejectsInvalidStatusFilter() throws Exception {
        mvc.perform(get(PATH + "?status=INVALID"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        mvc.perform(get(PATH + "?size=101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForProjectOutsideWorkspace() throws Exception {
        when(service.list(2L, 7L, null, 0, 20)).thenThrow(new ProjectNotFoundException());
        mvc.perform(get("/api/workspaces/2/projects/7/tasks"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForTaskOutsideProject() throws Exception {
        when(service.get(1L, 7L, 9L)).thenThrow(new TaskNotFoundException());
        mvc.perform(get(PATH + "/9")).andExpect(status().isNotFound());
    }

    @Test
    void changesStatus() throws Exception {
        mvc.perform(patch(PATH + "/9/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
        verify(service).changeStatus(1L, 7L, 9L, TaskStatus.DONE);
    }

    @Test
    void rejectsMissingStatus() throws Exception {
        mvc.perform(patch(PATH + "/9/status").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void updatesDetails() throws Exception {
        mvc.perform(patch(PATH + "/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New title\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isOk());
        verify(service).update(1L, 7L, 9L, "New title", null, TaskPriority.URGENT, null);
    }
}
