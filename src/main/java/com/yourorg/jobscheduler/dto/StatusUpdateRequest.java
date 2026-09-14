package com.yourorg.jobscheduler.dto;

import com.yourorg.jobscheduler.entity.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {

    @NotNull
    private JobStatus newStatus;
}