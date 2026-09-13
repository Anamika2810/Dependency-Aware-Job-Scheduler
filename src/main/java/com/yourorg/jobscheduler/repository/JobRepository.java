package com.yourorg.jobscheduler.repository;

import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);
}