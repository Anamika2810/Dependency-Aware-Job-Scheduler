package com.yourorg.jobscheduler.controller;

import com.yourorg.jobscheduler.dto.ExecutionResponse;
import com.yourorg.jobscheduler.dto.FailureRequest;
import com.yourorg.jobscheduler.execution.ExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manually triggers execution lifecycle events (Phase 4). Phase 5's
 * scheduler will eventually call startExecution automatically for
 * READY jobs; for now, these endpoints let the retry engine be
 * exercised and tested directly.
 */
@RestController
@RequestMapping("/api/jobs/{id}/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/start")
    public ResponseEntity<ExecutionResponse> start(@PathVariable("id") Long jobId) {
        var execution = executionService.startExecution(jobId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExecutionResponse.from(execution));
    }

    @PostMapping("/succeed")
    public ExecutionResponse succeed(@PathVariable("id") Long jobId) {
        return ExecutionResponse.from(executionService.recordSuccess(jobId));
    }

    @PostMapping("/fail")
    public ExecutionResponse fail(
            @PathVariable("id") Long jobId,
            @Valid @RequestBody FailureRequest request) {
        return ExecutionResponse.from(
            executionService.recordFailure(jobId, request.getErrorMessage()));
    }
}