package dev.relay.task;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByProject_Id(Long projectId, Pageable pageable);
    Page<Task> findByProject_IdAndStatus(Long projectId, TaskStatus status, Pageable pageable);
    Optional<Task> findByIdAndProject_IdAndProject_Workspace_Id(
            Long taskId, Long projectId, Long workspaceId);
}
