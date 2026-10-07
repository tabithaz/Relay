package dev.relay.incident;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentService {
    private final IncidentRepository incidents;
    private final IncidentActivityRepository activities;

    public IncidentService(IncidentRepository incidents, IncidentActivityRepository activities) {
        this.incidents = incidents;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public List<Incident> list() {
        return incidents.findAll();
    }

    public Incident create(String title, String description) {
        Incident incident = incidents.save(new Incident(title.trim(), description.trim()));
        activities.save(IncidentActivity.created(incident));
        return incident;
    }

    public Incident updateStatus(Long id, IncidentStatus nextStatus) {
        Incident incident = incidents.findById(id).orElseThrow(IncidentNotFoundException::new);
        if (incident.getStatus() == nextStatus) {
            return incident;
        }

        IncidentStatus previousStatus = incident.getStatus();
        incident.setStatus(nextStatus);
        Incident saved = incidents.save(incident);
        activities.save(IncidentActivity.statusChanged(saved, previousStatus));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<IncidentActivityResponse> activityFor(Long id) {
        if (!incidents.existsById(id)) {
            throw new IncidentNotFoundException();
        }
        return activities.findByIncidentIdOrderByOccurredAtAscIdAsc(id).stream()
                .map(IncidentActivityResponse::from)
                .toList();
    }
}
