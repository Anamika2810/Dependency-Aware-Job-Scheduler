package com.yourorg.jobscheduler.entity;

/**
 * Status of a single execution attempt. Deliberately a smaller set than
 * JobStatus — an execution doesn't have PENDING/READY/BLOCKED concepts,
 * it's either running, or it finished one way or another.
 */
public enum ExecutionStatus {
    RUNNING,
    SUCCESS,
    FAILED
}