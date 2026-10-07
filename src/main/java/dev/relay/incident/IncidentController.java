package dev.relay.incident;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Incident> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incident create(@Valid @RequestBody CreateIncident request) {
        return service.create(request.title(), request.description());
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate request) {
        return service.updateStatus(id, request.status());
    }

    @GetMapping("/{id}/activity")
    public List<IncidentActivityResponse> activity(@PathVariable Long id) {
        return service.activityFor(id);
    }

    public record CreateIncident(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 2000) String description
    ) {}

    public record StatusUpdate(@NotNull IncidentStatus status) {}
}
