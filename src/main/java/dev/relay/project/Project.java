package dev.relay.project;

import dev.relay.workspace.Workspace;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_projects_workspace_slug", columnNames = {"workspace_id", "slug"}),
        indexes = @Index(name = "idx_projects_workspace", columnList = "workspace_id"))
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_projects_workspace"))
    private Workspace workspace;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 63, updatable = false)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Project() {}

    public Project(Workspace workspace, String name, String slug, String description) {
        this.workspace = workspace;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
