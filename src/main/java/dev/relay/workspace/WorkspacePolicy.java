package dev.relay.workspace;

import org.springframework.stereotype.Component;

@Component("workspacePolicy")
public class WorkspacePolicy {
    private final WorkspaceMembershipRepository memberships;

    public WorkspacePolicy(WorkspaceMembershipRepository memberships) {
        this.memberships = memberships;
    }

    public boolean canRead(Long workspaceId, String subject) {
        return workspaceId != null && subject != null
                && memberships.existsByWorkspace_IdAndSubject(workspaceId, subject);
    }

    public boolean canWrite(Long workspaceId, String subject) {
        return role(workspaceId, subject).map(WorkspaceRole::canWrite).orElse(false);
    }

    public boolean canManageMembers(Long workspaceId, String subject) {
        return role(workspaceId, subject).filter(role -> role == WorkspaceRole.OWNER).isPresent();
    }

    private java.util.Optional<WorkspaceRole> role(Long workspaceId, String subject) {
        if (workspaceId == null || subject == null) {
            return java.util.Optional.empty();
        }
        return memberships.findByWorkspace_IdAndSubject(workspaceId, subject)
                .map(WorkspaceMembership::getRole);
    }
}
