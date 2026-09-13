package com.yourorg.jobscheduler.repository;

import com.yourorg.jobscheduler.entity.JobDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobDependencyRepository extends JpaRepository<JobDependency, Long> {

    /** All edges where the given job is the dependent (its upstream jobs). */
    List<JobDependency> findByJobId(Long jobId);

    /** All edges where the given job is upstream (its downstream dependents). */
    List<JobDependency> findByDependsOnJobId(Long dependsOnJobId);

    boolean existsByJobIdAndDependsOnJobId(Long jobId, Long dependsOnJobId);
}