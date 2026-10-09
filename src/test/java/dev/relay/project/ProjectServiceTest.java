package dev.relay.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.relay.workspace.Workspace;
import dev.relay.workspace.WorkspaceNotFoundException;
import dev.relay.workspace.WorkspaceRepository;
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
class ProjectServiceTest {
    @Mock ProjectRepository projects;
    @Mock WorkspaceRepository workspaces;
    @InjectMocks ProjectService service;

    private final Workspace workspace = new Workspace("Engineering", "engineering");

    @Test
    void createsProjectInsideWorkspaceAndNormalizesText() {
        when(workspaces.findById(1L)).thenReturn(Optional.of(workspace));
        when(projects.saveAndFlush(any(Project.class))).thenAnswer(call -> call.getArgument(0));

        ProjectResponse created = service.create(1L, "  Platform  ", "platform", "  Roadmap  ");

        assertEquals("Platform", created.name());
        assertEquals("Roadmap", created.description());
        assertEquals("platform", created.slug());
        assertNotNull(created.createdAt());
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projects).saveAndFlush(captor.capture());
        assertSame(workspace, captor.getValue().getWorkspace());
    }

    @Test
    void rejectsUnknownWorkspaceBeforeInserting() {
        when(workspaces.findById(404L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceNotFoundException.class,
                () -> service.create(404L, "Platform", "platform", null));
        verifyNoInteractions(projects);
    }

    @Test
    void rejectsDuplicateSlugOnlyWithinItsWorkspace() {
        when(workspaces.findById(1L)).thenReturn(Optional.of(workspace));
        when(projects.existsByWorkspace_IdAndSlug(1L, "platform")).thenReturn(true);

        assertThrows(ProjectSlugConflictException.class,
                () -> service.create(1L, "Platform", "platform", null));
        verify(projects, never()).saveAndFlush(any());
        verify(projects).existsByWorkspace_IdAndSlug(1L, "platform");
    }

    @Test
    void translatesConcurrentSlugCollisionToConflict() {
        when(workspaces.findById(1L)).thenReturn(Optional.of(workspace));
        when(projects.saveAndFlush(any(Project.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThrows(ProjectSlugConflictException.class,
                () -> service.create(1L, "Platform", "platform", null));
    }

    @Test
    void refusesProjectLookupOutsideItsWorkspace() {
        when(projects.findByIdAndWorkspace_Id(7L, 2L)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> service.get(2L, 7L));
        verify(projects, never()).findById(any());
    }

    @Test
    void updatesDetailsWithoutChangingWorkspaceOrSlug() {
        Project existing = new Project(workspace, "Old", "platform", "Before");
        when(projects.findByIdAndWorkspace_Id(7L, 1L)).thenReturn(Optional.of(existing));
        when(projects.save(existing)).thenReturn(existing);

        ProjectResponse updated = service.update(1L, 7L, "  New  ", "  After  ");

        assertEquals("New", updated.name());
        assertEquals("After", updated.description());
        assertEquals("platform", updated.slug());
        assertSame(workspace, existing.getWorkspace());
    }

    @Test
    void listsOnlyWorkspaceProjectsWithStablePagination() {
        when(workspaces.existsById(1L)).thenReturn(true);
        when(projects.findByWorkspace_Id(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Project(workspace, "Core", "core", null))));

        var page = service.list(1L, 2, 10);

        assertEquals(1, page.getContent().size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projects).findByWorkspace_Id(eq(1L), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertNotNull(captor.getValue().getSort().getOrderFor("createdAt"));
        assertNotNull(captor.getValue().getSort().getOrderFor("id"));
    }

    @Test
    void rejectsListingProjectsOfUnknownWorkspace() {
        when(workspaces.existsById(404L)).thenReturn(false);

        assertThrows(WorkspaceNotFoundException.class, () -> service.list(404L, 0, 20));
        verifyNoInteractions(projects);
    }
}
