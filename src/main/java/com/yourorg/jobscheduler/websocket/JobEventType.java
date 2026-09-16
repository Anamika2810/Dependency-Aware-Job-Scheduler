package com.yourorg.jobscheduler.websocket;

public enum JobEventType {
    JOB_READY,
    JOB_STARTED,
    JOB_SUCCEEDED,
    JOB_FAILED,
    JOB_RETRYING,
    JOB_PERMANENTLY_FAILED,
    JOB_BLOCKED
}