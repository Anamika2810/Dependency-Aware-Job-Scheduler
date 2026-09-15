package com.yourorg.jobscheduler.dto;

import com.yourorg.jobscheduler.entity.ExecutionStatus;
import com.yourorg.jobscheduler.entity.JobExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ExecutionResponse {

    private Long id;
    private Long jobId;
    private int attemptNumber;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private String errorMessage;
    private Instant nextRetryAt;

    public static ExecutionResponse from(JobExecution execution) {
        return ExecutionResponse.builder()
            .id(execution.getId())
            .jobId(execution.getJob().getId())
            .attemptNumber(execution.getAttemptNumber())
            .status(execution.getStatus())
            .startedAt(execution.getStartedAt())
            .finishedAt(execution.getFinishedAt())
            .errorMessage(execution.getErrorMessage())
            .nextRetryAt(execution.getNextRetryAt())
            .build();
    }
}