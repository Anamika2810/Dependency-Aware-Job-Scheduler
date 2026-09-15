package com.yourorg.jobscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailureRequest {

    @NotBlank
    private String errorMessage;
}