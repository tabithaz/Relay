package dev.relay.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long workspaceId, @PathVariable Long projectId,
                               @Valid @RequestBody CreateTask request) {
        return service.create(workspaceId, projectId, request.title(),
                request.description(), request.priority(), request.dueAt());
    }

    @GetMapping
    public Page<TaskResponse> list(@PathVariable Long workspaceId,
                                   @PathVariable Long projectId,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(workspaceId, projectId, status, page, size);
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable Long workspaceId, @PathVariable Long projectId,
                            @PathVariable Long taskId) {
        return service.get(workspaceId, projectId, taskId);
    }

    @PatchMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long workspaceId, @PathVariable Long projectId,
                               @PathVariable Long taskId,
                               @Valid @RequestBody UpdateTask request) {
        return service.update(workspaceId, projectId, taskId, request.title(),
                request.description(), request.priority(), request.dueAt());
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long workspaceId,
                                     @PathVariable Long projectId,
                                     @PathVariable Long taskId,
                                     @Valid @RequestBody ChangeTaskStatus request) {
        return service.changeStatus(workspaceId, projectId, taskId, request.status());
    }

    public record CreateTask(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 4000) String description,
            @NotNull TaskPriority priority,
            Instant dueAt) {}

    public record UpdateTask(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 4000) String description,
            @NotNull TaskPriority priority,
            Instant dueAt) {}

    public record ChangeTaskStatus(@NotNull TaskStatus status) {}
}
