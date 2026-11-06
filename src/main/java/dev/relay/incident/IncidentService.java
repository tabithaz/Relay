package dev.relay.incident;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
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
        Sort newestFirst = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return incidents.findAll(newestFirst);
    }

    public Incident create(String title, String description) {
        String normalizedTitle = Objects.requireNonNull(title, "title must not be null").trim();
        String normalizedDescription = Objects.requireNonNull(description, "description must not be null").trim();
        if (normalizedTitle.isEmpty() || normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("incident title and description must not be blank");
        }
        if (normalizedTitle.length() > 255 || normalizedDescription.length() > 2000) {
            throw new IllegalArgumentException("incident title or description exceeds maximum length");
        }
        Incident incident = incidents.save(new Incident(normalizedTitle, normalizedDescription));
        activities.save(IncidentActivity.created(incident));
        return incident;
    }

    public Incident updateStatus(Long id, IncidentStatus nextStatus) {
        Objects.requireNonNull(nextStatus, "nextStatus must not be null");
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
