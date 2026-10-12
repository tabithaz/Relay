package dev.relay.workspace;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, Long> {
    boolean existsByWorkspace_IdAndSubject(Long workspaceId, String subject);
    Optional<WorkspaceMembership> findByWorkspace_IdAndSubject(Long workspaceId, String subject);
    Page<WorkspaceMembership> findBySubject(String subject, Pageable pageable);
    Page<WorkspaceMembership> findByWorkspace_Id(Long workspaceId, Pageable pageable);
}
