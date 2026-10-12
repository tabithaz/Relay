package dev.relay.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {
    @Mock WorkspaceRepository workspaces;
    @Mock WorkspaceMembershipRepository memberships;
    @InjectMocks WorkspaceService service;

    @Test
    void createsWorkspaceWithTrimmedNameAndImmutableSlug() {
        when(workspaces.saveAndFlush(any(Workspace.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(memberships.saveAndFlush(any(WorkspaceMembership.class)))
                .thenAnswer(call -> call.getArgument(0));

        WorkspaceResponse created = service.create("  Product Team  ", "product-team", "user-42");

        assertEquals("Product Team", created.name());
        assertEquals("product-team", created.slug());
        assertNotNull(created.createdAt());
        ArgumentCaptor<Workspace> captor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaces).saveAndFlush(captor.capture());
        assertEquals("product-team", captor.getValue().getSlug());
        ArgumentCaptor<WorkspaceMembership> owner = ArgumentCaptor.forClass(WorkspaceMembership.class);
        verify(memberships).saveAndFlush(owner.capture());
        assertEquals("user-42", owner.getValue().getSubject());
        assertEquals(WorkspaceRole.OWNER, owner.getValue().getRole());
    }

    @Test
    void rejectsDuplicateSlugBeforeInserting() {
        when(workspaces.existsBySlug("product-team")).thenReturn(true);

        assertThrows(WorkspaceSlugConflictException.class,
                () -> service.create("Another team", "product-team", "user-42"));
        verify(workspaces, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentSlugCollisionToConflict() {
        when(workspaces.saveAndFlush(any(Workspace.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThrows(WorkspaceSlugConflictException.class,
                () -> service.create("Product Team", "product-team", "user-42"));
    }

    @Test
    void returnsNotFoundForUnknownWorkspace() {
        when(workspaces.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceNotFoundException.class, () -> service.get(99L));
    }

    @Test
    void renamesWorkspaceWithoutChangingSlug() {
        Workspace existing = new Workspace("Old Name", "team-alpha");
        when(workspaces.findById(5L)).thenReturn(Optional.of(existing));
        when(workspaces.save(existing)).thenReturn(existing);

        WorkspaceResponse updated = service.rename(5L, "  New Name  ");

        assertEquals("New Name", updated.name());
        assertEquals("team-alpha", updated.slug());
    }

    @Test
    void listsWorkspacesWithBoundedStablePagination() {
        when(memberships.findBySubject(eq("user-42"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new WorkspaceMembership(
                        new Workspace("Engineering", "engineering"), "user-42", WorkspaceRole.OWNER))));

        var page = service.list("user-42", 2, 10);

        assertEquals(1, page.getContent().size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(memberships).findBySubject(eq("user-42"), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertNotNull(captor.getValue().getSort().getOrderFor("workspace.createdAt"));
        assertNotNull(captor.getValue().getSort().getOrderFor("workspace.id"));
    }
}
