package com.yourorg.jobscheduler.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DependencyRequest {

    /** The id of the job that must complete first. */
    @NotNull
    private Long dependsOnJobId;
}