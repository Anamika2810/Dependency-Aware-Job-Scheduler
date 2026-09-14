package com.yourorg.jobscheduler.controller;

import com.yourorg.jobscheduler.dependency.JobDependencyService;
import com.yourorg.jobscheduler.dto.DependencyRequest;
import com.yourorg.jobscheduler.dto.JobRequest;
import com.yourorg.jobscheduler.dto.JobResponse;
import com.yourorg.jobscheduler.dto.StatusUpdateRequest;
import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobDependencyService dependencyService;

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request) {
        Job job = jobService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    public JobResponse getById(@PathVariable Long id) {
        return JobResponse.from(jobService.getById(id));
    }

    @GetMapping
    public List<JobResponse> getAll() {
        return jobService.getAll().stream().map(JobResponse::from).toList();
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return JobResponse.from(jobService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Moves a job's status, enforcing the state machine (Phase 3). */
    @PatchMapping("/{id}/status")
    public JobResponse transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return JobResponse.from(jobService.transitionStatus(id, request.getNewStatus()));
    }

    /** Adds a dependency edge: {id} depends on {request.dependsOnJobId}. */
    @PostMapping("/{id}/dependencies")
    public ResponseEntity<Void> addDependency(
            @PathVariable Long id,
            @Valid @RequestBody DependencyRequest request) {
        dependencyService.addDependency(id, request.getDependsOnJobId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    public ResponseEntity<Void> removeDependency(@PathVariable Long dependencyId) {
        dependencyService.removeDependency(dependencyId);
        return ResponseEntity.noContent().build();
    }

    /** Full valid execution order across every job in the system. */
    @GetMapping("/topological-order")
    public List<JobResponse> getTopologicalOrder() {
        return dependencyService.getTopologicalOrder().stream()
            .map(JobResponse::from)
            .toList();
    }

    /** Jobs that are PENDING with every dependency already SUCCESS. */
    @GetMapping("/ready")
    public List<JobResponse> getReadyJobs() {
        return dependencyService.getReadyJobs().stream()
            .map(JobResponse::from)
            .toList();
    }
}