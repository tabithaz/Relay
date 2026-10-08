package dev.relay.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "workspaces", uniqueConstraints = @UniqueConstraint(
        name = "uk_workspaces_slug", columnNames = "slug"))
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 63, updatable = false)
    private String slug;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Workspace() {}

    public Workspace(String name, String slug) {
        this.name = name;
        this.slug = slug;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }

    public void rename(String name) {
        this.name = name;
    }
}
