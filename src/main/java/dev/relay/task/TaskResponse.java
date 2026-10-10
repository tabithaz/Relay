package dev.relay.task;

import java.time.Instant;

public record TaskResponse(
        Long id, Long projectId, Long workspaceId, String title, String description,
        TaskStatus status, TaskPriority priority, Instant dueAt,
        Instant createdAt, Instant updatedAt, long version) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(), task.getProject().getId(),
                task.getProject().getWorkspace().getId(),
                task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getDueAt(), task.getCreatedAt(),
                task.getUpdatedAt(), task.getVersion());
    }
}
