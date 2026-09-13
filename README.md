# Dependency-Aware Job Scheduler

A Spring Boot monolith that models jobs as a DAG, enforces valid state
transitions, retries failures with exponential backoff, and pushes live
status updates over WebSocket.

## Prerequisites

- JDK 17 or 21
- Maven (or use the included `mvnw` wrapper once you generate one via
  `mvn -N io.takari:maven:wrapper`)
- MySQL 8.x running locally, or via Docker:
  ```
  docker run --name jobsched-mysql -e MYSQL_ROOT_PASSWORD=your_mysql_password \
    -p 3306:3306 -d mysql:8
  ```

## Setup

1. Update `src/main/resources/application.yml` with your MySQL
   username/password. The database `job_scheduler_db` will be
   created automatically on first run (`createDatabaseIfNotExist=true`).
2. Build and run:
   ```
   mvn spring-boot:run
   ```
3. The app starts on `http://localhost:8080`.

## Package layout

```
com.yourorg.jobscheduler
 ├── controller   REST endpoints (CRUD APIs)
 ├── service      Business logic
 ├── scheduler    @Scheduled polling (Phase 5)
 ├── execution    Job execution mechanism + JobExecution lifecycle
 ├── dependency   DAG logic: cycle detection, topological sort, READY/BLOCKED
 ├── repository   Spring Data JPA repositories
 ├── entity       Job, JobDependency, JobExecution
 ├── dto          Request/response payloads
 └── exception    Custom exceptions + global handler
```

## Build roadmap

| Phase | Focus |
|---|---|
| 1 | Core domain: `Job`, `JobDependency`, `JobExecution` entities + CRUD APIs |
| 2 | DAG engine: cycle detection, topological ordering, READY/BLOCKED eligibility |
| 3 | State machine: explicit valid transitions (PENDING → READY → RUNNING → ...) |
| 4 | Retry engine: exponential backoff (2s → 4s → 8s → permanent failure) |
| 5 | Scheduler: `@Scheduled` job that submits eligible READY jobs |
| 6 | WebSocket: push `JOB_STARTED` / `SUCCEEDED` / `FAILED` / `RETRYING` / `BLOCKED` |

Key design decisions:
- **`Job` (definition) is separate from `JobExecution` (a single run's
  history)** — so retries never overwrite prior attempts.
- Status transitions are validated against an explicit state machine
  rather than allowing arbitrary updates.
- Stays a modular monolith through v1; microservices/Redis/distributed
  locking are a deliberate *later* experiment, not a starting point.
