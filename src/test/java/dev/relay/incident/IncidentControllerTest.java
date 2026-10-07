package dev.relay.incident;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {
    @Autowired MockMvc mvc;
    @MockBean IncidentService service;

    @Test
    void rejectsBlankIncidentTitle() throws Exception {
        mvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  \",\"description\":\"Details\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingStatus() throws Exception {
        mvc.perform(patch("/api/incidents/7/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
