package com.yourorg.jobscheduler.execution;

import com.yourorg.jobscheduler.entity.ExecutionStatus;
import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobExecution;
import com.yourorg.jobscheduler.entity.JobStatus;
import com.yourorg.jobscheduler.exception.JobNotFoundException;
import com.yourorg.jobscheduler.repository.JobExecutionRepository;
import com.yourorg.jobscheduler.repository.JobRepository;
import com.yourorg.jobscheduler.service.JobService;
import com.yourorg.jobscheduler.websocket.JobEventPublisher;
import com.yourorg.jobscheduler.websocket.JobEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionService {

    private final JobExecutionRepository executionRepository;
    private final JobRepository jobRepository;
    private final JobService jobService;
    private final JobEventPublisher eventPublisher;

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

        eventPublisher.publish(JobEventType.JOB_STARTED, job.getId(), job.getName(),
            "Execution attempt " + nextAttemptNumber + " started");

        return execution;
    }

    public JobExecution recordSuccess(Long jobId) {
        JobExecution execution = latestExecution(jobId);
        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setFinishedAt(Instant.now());
        executionRepository.save(execution);

        Job job = jobService.transitionStatus(jobId, JobStatus.SUCCESS);

        eventPublisher.publish(JobEventType.JOB_SUCCEEDED, job.getId(), job.getName(),
            "Execution attempt " + execution.getAttemptNumber() + " succeeded");

        return execution;
    }

    public JobExecution recordFailure(Long jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));

        JobExecution execution = latestExecution(jobId);
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setFinishedAt(Instant.now());
        execution.setErrorMessage(errorMessage);

        jobService.transitionStatus(jobId, JobStatus.FAILED);
        eventPublisher.publish(JobEventType.JOB_FAILED, job.getId(), job.getName(),
            "Execution attempt " + execution.getAttemptNumber() + " failed: " + errorMessage);

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