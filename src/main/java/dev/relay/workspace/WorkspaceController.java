package dev.relay.workspace;

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
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@Valid @RequestBody CreateWorkspace request) {
        return service.create(request.name(), request.slug());
    }

    @GetMapping
    public Page<WorkspaceResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public WorkspaceResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public WorkspaceResponse rename(@PathVariable Long id,
                                    @Valid @RequestBody RenameWorkspace request) {
        return service.rename(id, request.name());
    }

    public record CreateWorkspace(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(min = 3, max = 63)
            @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                    message = "must use lowercase letters, numbers, and single hyphens")
            String slug) {}

    public record RenameWorkspace(@NotBlank @Size(max = 120) String name) {}
}
