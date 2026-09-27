package dev.relay.incident;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentRepository incidents;
    public IncidentController(IncidentRepository incidents){this.incidents=incidents;}

    @GetMapping
    public List<Incident> list(){ return incidents.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incident create(@Valid @RequestBody CreateIncident request){
        return incidents.save(new Incident(request.title(), request.description()));
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @RequestBody StatusUpdate request){
        Incident incident=incidents.findById(id).orElseThrow(IncidentNotFoundException::new);
        incident.setStatus(request.status());
        return incidents.save(incident);
    }

    public record CreateIncident(@NotBlank String title, @NotBlank String description){}
    public record StatusUpdate(IncidentStatus status){}
}
