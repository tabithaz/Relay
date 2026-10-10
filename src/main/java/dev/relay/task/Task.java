package dev.relay.task;

import dev.relay.project.Project;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "project_tasks", indexes = {
        @Index(name = "idx_project_tasks_project", columnList = "project_id"),
        @Index(name = "idx_project_tasks_status", columnList = "project_id,status")
})
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_tasks_project"))
    private Project project;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    private Instant dueAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Task() {}

    public Task(Project project, String title, String description,
                TaskPriority priority, Instant dueAt) {
        this.project = Objects.requireNonNull(project);
        this.title = title;
        this.description = description;
        this.priority = Objects.requireNonNull(priority);
        this.status = TaskStatus.TODO;
        this.dueAt = dueAt;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    public Instant getDueAt() { return dueAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void updateDetails(String title, String description,
                              TaskPriority priority, Instant dueAt) {
        if (Objects.equals(this.title, title)
                && Objects.equals(this.description, description)
                && this.priority == priority
                && Objects.equals(this.dueAt, dueAt)) {
            return;
        }
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueAt = dueAt;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(TaskStatus status) {
        if (this.status != status) {
            this.status = Objects.requireNonNull(status);
            this.updatedAt = Instant.now();
        }
    }
}
