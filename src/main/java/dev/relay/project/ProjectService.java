package dev.relay.project;

import dev.relay.workspace.Workspace;
import dev.relay.workspace.WorkspaceNotFoundException;
import dev.relay.workspace.WorkspaceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projects;
    private final WorkspaceRepository workspaces;

    public ProjectService(ProjectRepository projects, WorkspaceRepository workspaces) {
        this.projects = projects;
        this.workspaces = workspaces;
    }

    public ProjectResponse create(Long workspaceId, String name, String slug,
                                  String description) {
        Workspace workspace = workspaces.findById(workspaceId)
                .orElseThrow(WorkspaceNotFoundException::new);
        if (projects.existsByWorkspace_IdAndSlug(workspaceId, slug)) {
            throw new ProjectSlugConflictException();
        }
        try {
            // Flush inside the transaction to catch concurrent slug collisions.
            Project project = new Project(workspace, name.trim(), slug, normalize(description));
            return ProjectResponse.from(projects.saveAndFlush(project));
        } catch (DataIntegrityViolationException exception) {
            throw new ProjectSlugConflictException();
        }
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Long workspaceId, int page, int size) {
        if (!workspaces.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException();
        }
        Sort order = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return projects.findByWorkspace_Id(workspaceId, PageRequest.of(page, size, order))
                .map(ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long workspaceId, Long projectId) {
        return ProjectResponse.from(projects.findByIdAndWorkspace_Id(projectId, workspaceId)
                .orElseThrow(ProjectNotFoundException::new));
    }

    public ProjectResponse update(Long workspaceId, Long projectId,
                                  String name, String description) {
        Project project = projects.findByIdAndWorkspace_Id(projectId, workspaceId)
                .orElseThrow(ProjectNotFoundException::new);
        project.updateDetails(name.trim(), normalize(description));
        return ProjectResponse.from(projects.save(project));
    }

    private static String normalize(String description) {
        return description == null ? null : description.trim();
    }
}
