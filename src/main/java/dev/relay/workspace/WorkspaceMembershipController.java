package dev.relay.workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
public class WorkspaceMembershipController {
    private final WorkspaceMembershipService service;

    public WorkspaceMembershipController(WorkspaceMembershipService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspacePolicy.canManageMembers(#workspaceId, authentication.name)")
    public WorkspaceMembershipService.MemberResponse add(
            @PathVariable Long workspaceId, @Valid @RequestBody AddMember request) {
        return service.add(workspaceId, request.subject(), request.role());
    }

    @GetMapping
    @PreAuthorize("@workspacePolicy.canManageMembers(#workspaceId, authentication.name)")
    public Page<WorkspaceMembershipService.MemberResponse> list(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(workspaceId, page, size);
    }

    public record AddMember(@NotBlank @Size(max = 255) String subject,
                            @NotNull WorkspaceRole role) {}
}
