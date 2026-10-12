package dev.relay.workspace;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkspaceService {
    private final WorkspaceRepository workspaces;
    private final WorkspaceMembershipRepository memberships;

    public WorkspaceService(WorkspaceRepository workspaces,
                            WorkspaceMembershipRepository memberships) {
        this.workspaces = workspaces;
        this.memberships = memberships;
    }

    public WorkspaceResponse create(String name, String slug, String subject) {
        if (workspaces.existsBySlug(slug)) {
            throw new WorkspaceSlugConflictException();
        }

        Workspace workspace;
        try {
            // Flush the workspace insert to detect a concurrent slug collision.
            workspace = workspaces.saveAndFlush(new Workspace(name.trim(), slug));
        } catch (DataIntegrityViolationException exception) {
            throw new WorkspaceSlugConflictException();
        }
        // Membership insertion is in the same transaction. If it fails, creation rolls back.
        memberships.saveAndFlush(new WorkspaceMembership(workspace, subject, WorkspaceRole.OWNER));
        return WorkspaceResponse.from(workspace);
    }

    @Transactional(readOnly = true)
    public Page<WorkspaceResponse> list(String subject, int page, int size) {
        return memberships.findBySubject(subject, PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "workspace.createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "workspace.id"))))
                .map(membership -> WorkspaceResponse.from(membership.getWorkspace()));
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(Long id) {
        return WorkspaceResponse.from(workspaces.findById(id)
                .orElseThrow(WorkspaceNotFoundException::new));
    }

    public WorkspaceResponse rename(Long id, String name) {
        Workspace workspace = workspaces.findById(id)
                .orElseThrow(WorkspaceNotFoundException::new);
        workspace.rename(name.trim());
        return WorkspaceResponse.from(workspaces.save(workspace));
    }
}
