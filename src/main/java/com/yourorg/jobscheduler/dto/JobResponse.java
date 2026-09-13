package com.yourorg.jobscheduler.dto;

import com.yourorg.jobscheduler.entity.Job;
import com.yourorg.jobscheduler.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class JobResponse {

    private Long id;
    private String name;
    private String type;
    private JobStatus status;
    private int maxRetries;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Long> dependsOnJobIds;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
            .id(job.getId())
            .name(job.getName())
            .type(job.getType())
            .status(job.getStatus())
            .maxRetries(job.getMaxRetries())
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .dependsOnJobIds(
                job.getDependencies().stream()
                    .map(dep -> dep.getDependsOnJob().getId())
                    .toList()
            )
            .build();
    }
}