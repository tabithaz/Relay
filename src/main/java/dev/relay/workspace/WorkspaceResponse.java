package dev.relay.workspace;

import java.time.Instant;

public record WorkspaceResponse(Long id, String name, String slug, Instant createdAt) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getCreatedAt());
    }
}
