package com.yourorg.jobscheduler.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobEventPublisher {

    private static final String TOPIC = "/topic/job-updates";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(JobEventType type, Long jobId, String jobName, String message) {
        messagingTemplate.convertAndSend(TOPIC, JobEvent.of(type, jobId, jobName, message));
    }
}