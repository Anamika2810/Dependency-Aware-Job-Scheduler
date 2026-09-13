package com.yourorg.jobscheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    @Min(0)
    private int maxRetries = 3;
}