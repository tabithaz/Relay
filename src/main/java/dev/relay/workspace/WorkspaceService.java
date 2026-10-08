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

    public WorkspaceService(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
    }

    public WorkspaceResponse create(String name, String slug) {
        if (workspaces.existsBySlug(slug)) {
            throw new WorkspaceSlugConflictException();
        }

        try {
            // Flush now so a concurrent insert with the same slug returns HTTP 409.
            return WorkspaceResponse.from(
                    workspaces.saveAndFlush(new Workspace(name.trim(), slug)));
        } catch (DataIntegrityViolationException exception) {
            throw new WorkspaceSlugConflictException();
        }
    }

    @Transactional(readOnly = true)
    public Page<WorkspaceResponse> list(int page, int size) {
        Sort order = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return workspaces.findAll(PageRequest.of(page, size, order))
                .map(WorkspaceResponse::from);
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
