package dev.relay.project;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByWorkspace_IdAndSlug(Long workspaceId, String slug);
    Optional<Project> findByIdAndWorkspace_Id(Long id, Long workspaceId);
    Page<Project> findByWorkspace_Id(Long workspaceId, Pageable pageable);
}
