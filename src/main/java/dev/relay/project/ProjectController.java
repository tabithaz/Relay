package dev.relay.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@PathVariable Long workspaceId,
                                  @Valid @RequestBody CreateProject request) {
        return service.create(workspaceId, request.name(), request.slug(), request.description());
    }

    @GetMapping
    public Page<ProjectResponse> list(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(workspaceId, page, size);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Long workspaceId, @PathVariable Long projectId) {
        return service.get(workspaceId, projectId);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long workspaceId,
                                  @PathVariable Long projectId,
                                  @Valid @RequestBody UpdateProject request) {
        return service.update(workspaceId, projectId, request.name(), request.description());
    }

    public record CreateProject(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(min = 3, max = 63)
            @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "must use lowercase letters, numbers, and single hyphens")
            String slug,
            @Size(max = 2000) String description) {}

    public record UpdateProject(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description) {}
}
