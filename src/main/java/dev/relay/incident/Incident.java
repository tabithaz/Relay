package dev.relay.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(nullable = false, length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private IncidentStatus status;
    @Column(nullable = false) private Instant createdAt;

    protected Incident() {}
    public Incident(String title, String description) {
        this.title = title; this.description = description;
        this.status = IncidentStatus.INVESTIGATING; this.createdAt = Instant.now();
    }
    public Long getId(){return id;} public String getTitle(){return title;}
    public String getDescription(){return description;} public IncidentStatus getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;}
    public void setStatus(IncidentStatus status){this.status=status;}
}
