package com.yourorg.jobscheduler.service;

import com.yourorg.jobscheduler.dto.JobRequest;
import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobStatus;
import com.yourorg.jobscheduler.exception.InvalidStateTransitionException;
import com.yourorg.jobscheduler.exception.JobNotFoundException;
import com.yourorg.jobscheduler.repository.JobRepository;
import com.yourorg.jobscheduler.websocket.JobEventPublisher;
import com.yourorg.jobscheduler.websocket.JobEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventPublisher eventPublisher;

    private static final Map<JobStatus, JobEventType> STATUS_TO_EVENT = new EnumMap<>(JobStatus.class);

    static {
        STATUS_TO_EVENT.put(JobStatus.READY, JobEventType.JOB_READY);
        STATUS_TO_EVENT.put(JobStatus.RETRYING, JobEventType.JOB_RETRYING);
        STATUS_TO_EVENT.put(JobStatus.PERMANENT_FAILURE, JobEventType.JOB_PERMANENTLY_FAILED);
    }

    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS =
        new EnumMap<>(JobStatus.class);

    static {
        VALID_TRANSITIONS.put(JobStatus.PENDING, EnumSet.of(JobStatus.READY));
        VALID_TRANSITIONS.put(JobStatus.READY, EnumSet.of(JobStatus.RUNNING));
        VALID_TRANSITIONS.put(JobStatus.RUNNING, EnumSet.of(JobStatus.SUCCESS, JobStatus.FAILED));
        VALID_TRANSITIONS.put(JobStatus.FAILED, EnumSet.of(JobStatus.RETRYING, JobStatus.PERMANENT_FAILURE));
        VALID_TRANSITIONS.put(JobStatus.RETRYING, EnumSet.of(JobStatus.READY));
        VALID_TRANSITIONS.put(JobStatus.SUCCESS, EnumSet.noneOf(JobStatus.class));
        VALID_TRANSITIONS.put(JobStatus.PERMANENT_FAILURE, EnumSet.noneOf(JobStatus.class));
    }

    public Job create(JobRequest request) {
        Job job = Job.builder()
            .name(request.getName())
            .type(request.getType())
            .maxRetries(request.getMaxRetries())
            .build();
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Job getById(Long id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Job> getAll() {
        return jobRepository.findAll();
    }

    public Job update(Long id, JobRequest request) {
        Job job = getById(id);
        job.setName(request.getName());
        job.setType(request.getType());
        job.setMaxRetries(request.getMaxRetries());
        return jobRepository.save(job);
    }

    public void delete(Long id) {
        Job job = getById(id);
        jobRepository.delete(job);
    }

    public Job transitionStatus(Long id, JobStatus newStatus) {
        Job job = getById(id);
        JobStatus currentStatus = job.getStatus();

        Set<JobStatus> allowedNextStatuses =
            VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(JobStatus.class));

        if (!allowedNextStatuses.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                "Cannot transition job " + id + " from " + currentStatus
                    + " to " + newStatus + ". Valid next states from "
                    + currentStatus + " are: " + allowedNextStatuses);
        }

        job.setStatus(newStatus);
        Job saved = jobRepository.save(job);

        JobEventType eventType = STATUS_TO_EVENT.get(newStatus);
        if (eventType != null) {
            eventPublisher.publish(eventType, saved.getId(), saved.getName(),
                "Job moved to " + newStatus);
        }

        return saved;
    }
}