package com.yourorg.jobscheduler.dependency;

import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobDependency;
import com.yourorg.jobscheduler.exception.CyclicDependencyException;
import com.yourorg.jobscheduler.exception.JobNotFoundException;
import com.yourorg.jobscheduler.repository.JobDependencyRepository;
import com.yourorg.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Owns the JobDependency edges: CRUD plus cycle detection (Phase 2).
 * Topological sort and READY/BLOCKED eligibility are the remaining
 * Phase 2 items, layered on top of the same adjacency map built here.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JobDependencyService {

    private final JobDependencyRepository dependencyRepository;
    private final JobRepository jobRepository;

    public JobDependency addDependency(Long jobId, Long dependsOnJobId) {
        if (jobId.equals(dependsOnJobId)) {
            throw new IllegalArgumentException("A job cannot depend on itself");
        }

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));
        Job dependsOnJob = jobRepository.findById(dependsOnJobId)
            .orElseThrow(() -> new JobNotFoundException(dependsOnJobId));

        if (dependencyRepository.existsByJobIdAndDependsOnJobId(jobId, dependsOnJobId)) {
            throw new IllegalArgumentException("This dependency edge already exists");
        }

        if (wouldCreateCycle(jobId, dependsOnJobId)) {
            throw new CyclicDependencyException(
                "Adding this dependency would create a cycle: job " + jobId
                    + " -> " + dependsOnJobId);
        }

        JobDependency dependency = JobDependency.builder()
            .job(job)
            .dependsOnJob(dependsOnJob)
            .build();

        return dependencyRepository.save(dependency);
    }

    /**
     * Builds the adjacency map from existing edges, adds the proposed
     * (jobId -> dependsOnJobId) edge, then DFS's for a cycle. Since the
     * graph was acyclic before this call, a cycle can only appear if
     * dependsOnJobId can already reach jobId -- but running full cycle
     * detection keeps this correct even if that assumption ever changes.
     */
    private boolean wouldCreateCycle(Long jobId, Long dependsOnJobId) {
        Map<Long, List<Long>> graph = new HashMap<>();
        for (JobDependency edge : dependencyRepository.findAll()) {
            graph.computeIfAbsent(edge.getJob().getId(), k -> new ArrayList<>())
                .add(edge.getDependsOnJob().getId());
        }
        graph.computeIfAbsent(jobId, k -> new ArrayList<>()).add(dependsOnJobId);

        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();
        for (Long node : graph.keySet()) {
            if (detectCycle(node, graph, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCycle(
            Long node,
            Map<Long, List<Long>> graph,
            Set<Long> visited,
            Set<Long> recursionStack) {

        if (recursionStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recursionStack.add(node);

        for (Long neighbor : graph.getOrDefault(node, Collections.emptyList())) {
            if (detectCycle(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }

    public void removeDependency(Long dependencyId) {
        if (!dependencyRepository.existsById(dependencyId)) {
            throw new IllegalArgumentException("Dependency edge not found with id: " + dependencyId);
        }
        dependencyRepository.deleteById(dependencyId);
    }
}