package dev.relay.project;

import java.time.Instant;

public record ProjectResponse(
        Long id, Long workspaceId, String name, String slug,
        String description, Instant createdAt) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(), project.getWorkspace().getId(),
                project.getName(), project.getSlug(),
                project.getDescription(), project.getCreatedAt());
    }
}
