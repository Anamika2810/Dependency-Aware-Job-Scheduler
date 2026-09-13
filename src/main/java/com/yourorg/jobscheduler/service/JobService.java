package com.yourorg.jobscheduler.service;

import com.yourorg.jobscheduler.dto.JobRequest;
import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.exception.JobNotFoundException;
import com.yourorg.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobService {

    private final JobRepository jobRepository;

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
}