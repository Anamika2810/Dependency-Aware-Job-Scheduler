package com.yourorg.jobscheduler.execution;

import com.yourorg.jobscheduler.entity.ExecutionStatus;
import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobExecution;
import com.yourorg.jobscheduler.entity.JobStatus;
import com.yourorg.jobscheduler.exception.JobNotFoundException;
import com.yourorg.jobscheduler.repository.JobExecutionRepository;
import com.yourorg.jobscheduler.repository.JobRepository;
import com.yourorg.jobscheduler.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The retry engine (Phase 4). Manages the lifecycle of individual
 * execution attempts, and computes exponential backoff on failure:
 * 2s, 4s, 8s, ... doubling per attempt, up to the job's maxRetries,
 * after which the job is marked PERMANENT_FAILURE.
 *
 * Each call here creates or updates a JobExecution row -- never the
 * Job itself -- so every attempt's history survives (see the
 * Job/JobExecution separation from Phase 1).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionService {

    private final JobExecutionRepository executionRepository;
    private final JobRepository jobRepository;
    private final JobService jobService;

    /**
     * Starts a new execution attempt: creates a JobExecution row with
     * the next attempt number, and moves the job from READY to
     * RUNNING via the state machine.
     */
    public JobExecution startExecution(Long jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));

        int nextAttemptNumber = executionRepository
            .findTopByJobIdOrderByAttemptNumberDesc(jobId)
            .map(previous -> previous.getAttemptNumber() + 1)
            .orElse(1);

        JobExecution execution = JobExecution.builder()
            .job(job)
            .attemptNumber(nextAttemptNumber)
            .status(ExecutionStatus.RUNNING)
            .startedAt(Instant.now())
            .build();

        executionRepository.save(execution);
        jobService.transitionStatus(jobId, JobStatus.RUNNING);

        return execution;
    }

    /**
     * Marks the most recent execution attempt as SUCCESS, and moves
     * the job from RUNNING to SUCCESS.
     */
    public JobExecution recordSuccess(Long jobId) {
        JobExecution execution = latestExecution(jobId);
        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setFinishedAt(Instant.now());
        executionRepository.save(execution);

        jobService.transitionStatus(jobId, JobStatus.SUCCESS);

        return execution;
    }

    /**
     * Marks the most recent execution attempt as FAILED, then decides
     * what happens next:
     *   - if attempts remain (attemptNumber < job.maxRetries): compute
     *     the next backoff window (2^attemptNumber seconds) and move
     *     the job to RETRYING
     *   - otherwise: move the job to PERMANENT_FAILURE
     *
     * A future retry attempt (Phase 5's scheduler) will eventually
     * call startExecution again once nextRetryAt has elapsed and the
     * job has been moved back to READY.
     */
    public JobExecution recordFailure(Long jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));

        JobExecution execution = latestExecution(jobId);
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setFinishedAt(Instant.now());
        execution.setErrorMessage(errorMessage);

        jobService.transitionStatus(jobId, JobStatus.FAILED);

        if (execution.getAttemptNumber() < job.getMaxRetries()) {
            long backoffSeconds = (long) Math.pow(2, execution.getAttemptNumber());
            execution.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
            executionRepository.save(execution);

            jobService.transitionStatus(jobId, JobStatus.RETRYING);
        } else {
            executionRepository.save(execution);
            jobService.transitionStatus(jobId, JobStatus.PERMANENT_FAILURE);
        }

        return execution;
    }

    private JobExecution latestExecution(Long jobId) {
        return executionRepository.findTopByJobIdOrderByAttemptNumberDesc(jobId)
            .orElseThrow(() -> new IllegalStateException(
                "No execution attempts found for job " + jobId
                    + " -- call startExecution first"));
    }
}