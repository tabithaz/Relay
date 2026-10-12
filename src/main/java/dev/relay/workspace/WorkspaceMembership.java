package dev.relay.workspace;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workspace_memberships", uniqueConstraints = @UniqueConstraint(
        name = "uk_workspace_memberships_workspace_subject",
        columnNames = {"workspace_id", "member_subject"}))
public class WorkspaceMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "member_subject", nullable = false, length = 255, updatable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkspaceRole role;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected WorkspaceMembership() {}

    public WorkspaceMembership(Workspace workspace, String subject, WorkspaceRole role) {
        this.workspace = workspace;
        this.subject = subject;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public String getSubject() { return subject; }
    public WorkspaceRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
