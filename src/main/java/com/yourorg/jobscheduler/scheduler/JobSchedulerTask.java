package com.yourorg.jobscheduler.scheduler;

import com.yourorg.jobscheduler.dependency.JobDependencyService;
import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobExecution;
import com.yourorg.jobscheduler.entity.JobStatus;
import com.yourorg.jobscheduler.execution.ExecutionService;
import com.yourorg.jobscheduler.repository.JobExecutionRepository;
import com.yourorg.jobscheduler.repository.JobRepository;
import com.yourorg.jobscheduler.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The scheduler (Phase 5). Runs on a fixed interval and does three
 * things, in order, each cycle:
 *
 *   1. Promote PENDING jobs whose dependencies have all SUCCEEDED to
 *      READY (reuses the DAG eligibility logic from Phase 2).
 *   2. Promote RETRYING jobs whose backoff window (nextRetryAt) has
 *      elapsed back to READY, so they get picked up again below.
 *   3. Start execution on every job that is now READY.
 *
 * This is what turns the project from "a set of manually-triggered
 * API calls" into an actually autonomous scheduler -- everything
 * before this phase required a human (or a test) to call each step.
 */
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JobSchedulerTask {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobDependencyService dependencyService;
    private final JobService jobService;
    private final ExecutionService executionService;

    /**
     * fixedDelay (not fixedRate) is used deliberately: it waits for
     * one run to fully finish before starting the timer for the next,
     * so a slow cycle can never overlap with itself.
     */
    @Scheduled(fixedDelay = 5000)
    public void runSchedulingCycle() {
        promotePendingJobsToReady();
        promoteExpiredRetriesToReady();
        startAllReadyJobs();
    }

    private void promotePendingJobsToReady() {
        for (Job job : dependencyService.getReadyJobs()) {
            log.info("Promoting job {} from PENDING to READY (dependencies satisfied)", job.getId());
            jobService.transitionStatus(job.getId(), JobStatus.READY);
        }
    }

    private void promoteExpiredRetriesToReady() {
        Instant now = Instant.now();

        for (JobExecution execution : executionRepository.findByNextRetryAtLessThanEqual(now)) {
            Job job = execution.getJob();

            if (job.getStatus() != JobStatus.RETRYING) {
                // Already handled in a previous cycle, or moved on some
                // other way -- skip to avoid an invalid transition.
                continue;
            }

            log.info("Backoff elapsed for job {}, promoting RETRYING to READY", job.getId());

            // Clear nextRetryAt so this same execution row doesn't match
            // the query again on the next cycle.
            execution.setNextRetryAt(null);
            executionRepository.save(execution);

            jobService.transitionStatus(job.getId(), JobStatus.READY);
        }
    }

    private void startAllReadyJobs() {
        for (Job job : jobRepository.findByStatus(JobStatus.READY)) {
            log.info("Auto-starting execution for READY job {}", job.getId());
            executionService.startExecution(job.getId());
        }
    }
}