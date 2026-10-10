package dev.relay.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.relay.project.Project;
import dev.relay.project.ProjectNotFoundException;
import dev.relay.project.ProjectRepository;
import dev.relay.workspace.Workspace;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository tasks;
    @Mock ProjectRepository projects;
    @InjectMocks TaskService service;

    private final Project project = new Project(
            new Workspace("Engineering", "engineering"), "Platform", "platform", null);

    @Test
    void createsTaskWithDefaultStatusAndNormalizedText() {
        when(projects.findByIdAndWorkspace_Id(7L, 1L)).thenReturn(Optional.of(project));
        when(tasks.save(any(Task.class))).thenAnswer(call -> call.getArgument(0));

        TaskResponse created = service.create(1L, 7L, "  Fix login  ",
                "  Handle timeouts  ", TaskPriority.HIGH, null);

        assertEquals("Fix login", created.title());
        assertEquals("Handle timeouts", created.description());
        assertEquals(TaskStatus.TODO, created.status());
        assertEquals(TaskPriority.HIGH, created.priority());
        assertNotNull(created.createdAt());
        assertEquals(created.createdAt(), created.updatedAt());
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(tasks).save(captor.capture());
        assertSame(project, captor.getValue().getProject());
    }

    @Test
    void refusesTaskCreationForProjectInAnotherWorkspace() {
        when(projects.findByIdAndWorkspace_Id(7L, 2L)).thenReturn(Optional.empty());
        assertThrows(ProjectNotFoundException.class,
                () -> service.create(2L, 7L, "Fix login", null, TaskPriority.NORMAL, null));
        verifyNoInteractions(tasks);
    }

    @Test
    void listsTasksWithStablePaginationAndStatusFilter() {
        when(projects.findByIdAndWorkspace_Id(7L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findByProject_IdAndStatus(eq(7L), eq(TaskStatus.BLOCKED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new Task(project, "Fix login", null, TaskPriority.NORMAL, null))));
        var result = service.list(1L, 7L, TaskStatus.BLOCKED, 2, 10);
        assertEquals(1, result.getContent().size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tasks).findByProject_IdAndStatus(eq(7L), eq(TaskStatus.BLOCKED), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertNotNull(captor.getValue().getSort().getOrderFor("createdAt"));
        assertNotNull(captor.getValue().getSort().getOrderFor("id"));
        verify(tasks, never()).findByProject_Id(anyLong(), any());
    }

    @Test
    void listsAllStatusesWhenFilterOmitted() {
        when(projects.findByIdAndWorkspace_Id(7L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findByProject_Id(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(service.list(1L, 7L, null, 0, 20).isEmpty());
        verify(tasks, never()).findByProject_IdAndStatus(anyLong(), any(), any());
    }

    @Test
    void rejectsTaskLookupOutsideItsProjectOrWorkspace() {
        when(tasks.findByIdAndProject_IdAndProject_Workspace_Id(9L, 7L, 2L))
                .thenReturn(Optional.empty());
        assertThrows(TaskNotFoundException.class, () -> service.get(2L, 7L, 9L));
        verify(tasks, never()).findById(anyLong());
    }

    @Test
    void changesStatusWithoutMovingTask() {
        Task task = new Task(project, "Fix login", null, TaskPriority.HIGH, null);
        when(tasks.findByIdAndProject_IdAndProject_Workspace_Id(9L, 7L, 1L))
                .thenReturn(Optional.of(task));
        when(tasks.save(task)).thenReturn(task);

        TaskResponse updated = service.changeStatus(1L, 7L, 9L, TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, updated.status());
        assertSame(project, task.getProject());
        verify(tasks).save(task);
    }

    @Test
    void repeatedStatusUpdateDoesNotWriteAgain() {
        Task task = new Task(project, "Fix login", null, TaskPriority.HIGH, null);
        when(tasks.findByIdAndProject_IdAndProject_Workspace_Id(9L, 7L, 1L))
                .thenReturn(Optional.of(task));

        TaskResponse unchanged = service.changeStatus(1L, 7L, 9L, TaskStatus.TODO);

        assertEquals(TaskStatus.TODO, unchanged.status());
        verify(tasks, never()).save(any());
    }

    @Test
    void updatesTaskDetailsAndClearsOptionalFields() {
        Instant due = Instant.parse("2026-11-01T12:00:00Z");
        Task task = new Task(project, "Old", "Old description", TaskPriority.LOW, due);
        when(tasks.findByIdAndProject_IdAndProject_Workspace_Id(9L, 7L, 1L))
                .thenReturn(Optional.of(task));
        when(tasks.save(task)).thenReturn(task);

        TaskResponse updated = service.update(1L, 7L, 9L, "  New  ",
                "   ", TaskPriority.URGENT, null);

        assertEquals("New", updated.title());
        assertNull(updated.description());
        assertNull(updated.dueAt());
        assertEquals(TaskPriority.URGENT, updated.priority());
        assertSame(project, task.getProject());
    }
}
