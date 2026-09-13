package com.yourorg.jobscheduler.dependency;

import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobDependency;
import com.yourorg.jobscheduler.entity.JobStatus;
import com.yourorg.jobscheduler.exception.CyclicDependencyException;
import com.yourorg.jobscheduler.exception.JobNotFoundException;
import com.yourorg.jobscheduler.repository.JobDependencyRepository;
import com.yourorg.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Owns the JobDependency edges: CRUD, cycle detection, topological
 * ordering, and READY/BLOCKED eligibility -- the full DAG engine
 * (Phase 2).
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

    /**
     * Returns every job in a valid execution order, using Kahn's
     * algorithm (repeatedly peel off nodes with in-degree zero). If
     * fewer jobs come out than exist in the graph, a cycle is present
     * -- which shouldn't be reachable given addDependency's guard, but
     * this makes the method safe to call independently and doubles as
     * a second line of defense.
     */
    @Transactional(readOnly = true)
    public List<Job> getTopologicalOrder() {
        List<Job> allJobs = jobRepository.findAll();
        List<JobDependency> allEdges = dependencyRepository.findAll();

        // in-degree here = number of jobs THIS job depends on that
        // haven't been "removed" from the graph yet.
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, List<Long>> dependents = new HashMap<>(); // dependsOnJobId -> jobs waiting on it
        Map<Long, Job> jobsById = new HashMap<>();

        for (Job job : allJobs) {
            inDegree.put(job.getId(), 0);
            jobsById.put(job.getId(), job);
        }
        for (JobDependency edge : allEdges) {
            Long jobId = edge.getJob().getId();
            Long dependsOnJobId = edge.getDependsOnJob().getId();
            inDegree.merge(jobId, 1, Integer::sum);
            dependents.computeIfAbsent(dependsOnJobId, k -> new ArrayList<>()).add(jobId);
        }

        Deque<Long> queue = new ArrayDeque<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Job> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            ordered.add(jobsById.get(current));

            for (Long dependent : dependents.getOrDefault(current, Collections.emptyList())) {
                int remaining = inDegree.merge(dependent, -1, Integer::sum);
                if (remaining == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (ordered.size() != allJobs.size()) {
            throw new CyclicDependencyException(
                "Cycle detected while computing topological order -- "
                    + "graph should have been kept acyclic by addDependency");
        }

        return ordered;
    }

    /**
     * A job is READY when it's still PENDING and every job it depends
     * on has reached SUCCESS. Jobs with no dependencies at all are
     * trivially READY. Everything else PENDING is BLOCKED (not
     * returned here).
     */
    @Transactional(readOnly = true)
    public List<Job> getReadyJobs() {
        List<Job> readyJobs = new ArrayList<>();

        for (Job job : jobRepository.findByStatus(JobStatus.PENDING)) {
            List<JobDependency> upstream = dependencyRepository.findByJobId(job.getId());

            boolean allDependenciesSucceeded = upstream.stream()
                .allMatch(edge -> edge.getDependsOnJob().getStatus() == JobStatus.SUCCESS);

            if (allDependenciesSucceeded) {
                readyJobs.add(job);
            }
        }

        return readyJobs;
    }
}