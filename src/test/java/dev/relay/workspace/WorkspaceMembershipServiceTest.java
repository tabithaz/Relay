package dev.relay.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WorkspaceMembershipServiceTest {
    @Mock WorkspaceRepository workspaces;
    @Mock WorkspaceMembershipRepository memberships;
    @InjectMocks WorkspaceMembershipService service;

    @Test
    void addsEditorWithNormalizedSubject() {
        Workspace workspace = new Workspace("Team", "team");
        when(workspaces.findById(1L)).thenReturn(Optional.of(workspace));
        when(memberships.saveAndFlush(any(WorkspaceMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.add(1L, "  user-77  ", WorkspaceRole.EDITOR);
        assertEquals("user-77", response.subject());
        assertEquals(WorkspaceRole.EDITOR, response.role());
        assertNotNull(response.joinedAt());
        verify(memberships).existsByWorkspace_IdAndSubject(1L, "user-77");
    }

    @Test
    void cannotAssignOwnerRole() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.add(1L, "user-77", WorkspaceRole.OWNER));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(workspaces, memberships);
    }

    @Test
    void duplicateMemberReturnsConflict() {
        when(workspaces.findById(1L)).thenReturn(Optional.of(new Workspace("Team", "team")));
        when(memberships.existsByWorkspace_IdAndSubject(1L, "user-77")).thenReturn(true);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.add(1L, "user-77", WorkspaceRole.VIEWER));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateReturnsConflict() {
        when(workspaces.findById(1L)).thenReturn(Optional.of(new Workspace("Team", "team")));
        when(memberships.saveAndFlush(any(WorkspaceMembership.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.add(1L, "user-77", WorkspaceRole.VIEWER));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void unknownWorkspaceReturnsNotFound() {
        assertThrows(WorkspaceNotFoundException.class,
                () -> service.add(99L, "user-77", WorkspaceRole.VIEWER));
        verify(memberships, never()).saveAndFlush(any());
    }
}
