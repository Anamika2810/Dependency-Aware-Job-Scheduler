package com.yourorg.jobscheduler.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class JobEvent {

    private JobEventType type;
    private Long jobId;
    private String jobName;
    private String message;
    private Instant timestamp;

    public static JobEvent of(JobEventType type, Long jobId, String jobName, String message) {
        return JobEvent.builder()
            .type(type)
            .jobId(jobId)
            .jobName(jobName)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
}