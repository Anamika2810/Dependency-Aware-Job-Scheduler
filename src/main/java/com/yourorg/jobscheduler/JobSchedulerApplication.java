package com.yourorg.jobscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Dependency-Aware Job Scheduler.
 *
 * @EnableScheduling is turned on from day one so Phase 5's @Scheduled
 * polling job works without needing to revisit this file later.
 */
@SpringBootApplication
@EnableScheduling
public class JobSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobSchedulerApplication.class, args);
    }

}
