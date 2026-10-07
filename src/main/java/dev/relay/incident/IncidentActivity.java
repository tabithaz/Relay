package dev.relay.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident_activities", indexes = {
    @Index(name = "idx_incident_activity_lookup", columnList = "incident_id,occurred_at,id")
})
public class IncidentActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private IncidentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private IncidentStatus currentStatus;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected IncidentActivity() {}

    private IncidentActivity(Incident incident, IncidentActivityType type,
                             IncidentStatus previousStatus, IncidentStatus currentStatus) {
        this.incident = incident;
        this.type = type;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = Instant.now();
    }

    public static IncidentActivity created(Incident incident) {
        return new IncidentActivity(incident, IncidentActivityType.CREATED, null, incident.getStatus());
    }

    public static IncidentActivity statusChanged(Incident incident, IncidentStatus previousStatus) {
        return new IncidentActivity(incident, IncidentActivityType.STATUS_CHANGED,
                previousStatus, incident.getStatus());
    }

    public Long getId() { return id; }
    public IncidentActivityType getType() { return type; }
    public IncidentStatus getPreviousStatus() { return previousStatus; }
    public IncidentStatus getCurrentStatus() { return currentStatus; }
    public Instant getOccurredAt() { return occurredAt; }
}
