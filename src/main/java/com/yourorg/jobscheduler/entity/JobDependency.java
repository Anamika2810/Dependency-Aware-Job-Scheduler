package com.yourorg.jobscheduler.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A directed edge in the dependency graph: {@code job} depends on
 * {@code dependsOnJob}, meaning dependsOnJob must reach SUCCESS before
 * job can become READY.
 *
 * Example: if D depends on C, then:
 *   job         = D
 *   dependsOnJob = C
 *
 * The DAG engine (Phase 2) walks these edges to detect cycles, compute
 * topological order, and determine READY/BLOCKED eligibility.
 */
@Entity
@Table(
    name = "job_dependencies",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_job_dependency_edge",
        columnNames = {"job_id", "depends_on_job_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The job that has a dependency (the "downstream" job). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /** The job that must complete first (the "upstream" job). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_job_id", nullable = false)
    private Job dependsOnJob;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}