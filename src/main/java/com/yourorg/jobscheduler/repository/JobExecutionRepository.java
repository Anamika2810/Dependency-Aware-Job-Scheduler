package com.yourorg.jobscheduler.repository;

import com.yourorg.jobscheduler.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {

    List<JobExecution> findByJobIdOrderByAttemptNumberAsc(Long jobId);

    Optional<JobExecution> findTopByJobIdOrderByAttemptNumberDesc(Long jobId);

    /** Executions awaiting a retry whose backoff window has elapsed (Phase 4/5). */
    List<JobExecution> findByNextRetryAtLessThanEqual(Instant now);
}