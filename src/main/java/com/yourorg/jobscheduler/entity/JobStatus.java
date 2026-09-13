package com.yourorg.jobscheduler.entity;

/**
 * Job-level status, driven by the state machine (Phase 3).
 *
 * Valid transitions (enforced in the service layer, not here):
 *   PENDING   -> READY
 *   READY     -> RUNNING
 *   RUNNING   -> SUCCESS
 *   RUNNING   -> FAILED
 *   FAILED    -> RETRYING
 *   RETRYING  -> READY
 *   FAILED    -> PERMANENT_FAILURE
 */
public enum JobStatus {
    PENDING,
    READY,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    PERMANENT_FAILURE
}