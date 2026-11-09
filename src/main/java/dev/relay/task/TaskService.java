package dev.relay.task;

import dev.relay.project.Project;
import dev.relay.project.ProjectNotFoundException;
import dev.relay.project.ProjectRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;

    public TaskService(TaskRepository tasks, ProjectRepository projects) {
        this.tasks = tasks;
        this.projects = projects;
    }

    public TaskResponse create(Long workspaceId, Long projectId, String title,
                               String description, TaskPriority priority, Instant dueAt) {
        Objects.requireNonNull(priority, "priority");
        String normalizedTitle = validateTitle(title);
        String normalizedDescription = validateDescription(description);
        Project project = requireProject(workspaceId, projectId);
        Task task = new Task(project, normalizedTitle, normalizedDescription, priority, dueAt);
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Long workspaceId, Long projectId,
                                   TaskStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be nonnegative and size must be between 1 and 100");
        }
        requireProject(workspaceId, projectId);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        var pageable = PageRequest.of(page, size, sort);
        Page<Task> result = status == null
                ? tasks.findByProject_Id(projectId, pageable)
                : tasks.findByProject_IdAndStatus(projectId, status, pageable);
        return result.map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long workspaceId, Long projectId, Long taskId) {
        return TaskResponse.from(requireTask(workspaceId, projectId, taskId));
    }

    public TaskResponse update(Long workspaceId, Long projectId, Long taskId,
                               String title, String description, TaskPriority priority,
                               Instant dueAt) {
        Objects.requireNonNull(priority, "priority");
        String normalizedTitle = validateTitle(title);
        String normalizedDescription = validateDescription(description);
        Task task = requireTask(workspaceId, projectId, taskId);
        if (Objects.equals(task.getTitle(), normalizedTitle)
                && Objects.equals(task.getDescription(), normalizedDescription)
                && task.getPriority() == priority
                && Objects.equals(task.getDueAt(), dueAt)) {
            return TaskResponse.from(task);
        }
        task.updateDetails(normalizedTitle, normalizedDescription, priority, dueAt);
        return TaskResponse.from(tasks.save(task));
    }

    public TaskResponse changeStatus(Long workspaceId, Long projectId,
                                     Long taskId, TaskStatus status) {
        Objects.requireNonNull(status, "status");
        Task task = requireTask(workspaceId, projectId, taskId);
        if (task.getStatus() == status) {
            return TaskResponse.from(task);
        }
        task.changeStatus(status);
        return TaskResponse.from(tasks.save(task));
    }

    private Project requireProject(Long workspaceId, Long projectId) {
        return projects.findByIdAndWorkspace_Id(projectId, workspaceId)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private Task requireTask(Long workspaceId, Long projectId, Long taskId) {
        return tasks.findByIdAndProject_IdAndProject_Workspace_Id(
                taskId, projectId, workspaceId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private static String validateTitle(String title) {
        Objects.requireNonNull(title, "title");
        String trimmed = title.trim();
        if (trimmed.isEmpty() || trimmed.length() > 160) {
            throw new IllegalArgumentException("title must contain 1 to 160 characters");
        }
        return trimmed;
    }

    private static String validateDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > 4000) {
            throw new IllegalArgumentException("description must not exceed 4000 characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
