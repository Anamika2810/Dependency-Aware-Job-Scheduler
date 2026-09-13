package com.yourorg.jobscheduler.exception;

public class CyclicDependencyException extends RuntimeException {

    public CyclicDependencyException(String message) {
        super(message);
    }
}