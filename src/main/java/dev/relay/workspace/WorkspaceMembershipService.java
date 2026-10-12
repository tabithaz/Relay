package dev.relay.workspace;

import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class WorkspaceMembershipService {
    private final WorkspaceRepository workspaces;
    private final WorkspaceMembershipRepository memberships;

    public WorkspaceMembershipService(WorkspaceRepository workspaces,
                                      WorkspaceMembershipRepository memberships) {
        this.workspaces = workspaces;
        this.memberships = memberships;
    }

    public MemberResponse add(Long workspaceId, String subject, WorkspaceRole role) {
        if (role == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER cannot be assigned");
        }
        String normalized = subject.trim();
        Workspace workspace = workspaces.findById(workspaceId)
                .orElseThrow(WorkspaceNotFoundException::new);
        if (memberships.existsByWorkspace_IdAndSubject(workspaceId, normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member already exists");
        }
        try {
            return MemberResponse.from(memberships.saveAndFlush(
                    new WorkspaceMembership(workspace, normalized, role)));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member already exists");
        }
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> list(Long workspaceId, int page, int size) {
        return memberships.findByWorkspace_Id(workspaceId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(MemberResponse::from);
    }

    public record MemberResponse(String subject, WorkspaceRole role, Instant joinedAt) {
        static MemberResponse from(WorkspaceMembership member) {
            return new MemberResponse(member.getSubject(), member.getRole(), member.getJoinedAt());
        }
    }
}
