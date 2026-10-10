package dev.relay.task;

import dev.relay.project.Project;
import dev.relay.project.ProjectNotFoundException;
import dev.relay.project.ProjectRepository;
import java.time.Instant;
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
        Project project = requireProject(workspaceId, projectId);
        Task task = new Task(project, title.trim(), normalize(description), priority, dueAt);
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Long workspaceId, Long projectId,
                                   TaskStatus status, int page, int size) {
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
        Task task = requireTask(workspaceId, projectId, taskId);
        task.updateDetails(title.trim(), normalize(description), priority, dueAt);
        return TaskResponse.from(tasks.save(task));
    }

    public TaskResponse changeStatus(Long workspaceId, Long projectId,
                                     Long taskId, TaskStatus status) {
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

    private static String normalize(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
