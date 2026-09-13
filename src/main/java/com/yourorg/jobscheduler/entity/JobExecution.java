package com.yourorg.jobscheduler.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single execution attempt of a Job. Every retry creates a NEW row
 * here rather than overwriting the previous one, so full attempt
 * history is preserved.
 *
 * The retry-specific fields (attemptNumber, nextRetryAt, lastError) are
 * included now, even though the retry engine itself is Phase 4, so the
 * schema doesn't need to change later.
 */
@Entity
@Table(name = "job_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /** 1 for the first attempt, 2 for the first retry, etc. */
    @Column(nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    private Instant startedAt;

    private Instant finishedAt;

    /** Populated only on failure. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * When this job is next eligible to be retried, computed via
     * exponential backoff (2s, 4s, 8s, ...) in Phase 4. Null when not
     * awaiting retry.
     */
    private Instant nextRetryAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}