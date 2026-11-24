package com.example.reconciliation.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Run the job by hand instead of waiting for 01:30:
 *   curl -X POST "http://localhost:8080/jobs/reconcile?businessDate=2026-11-24"
 *
 * The default JobLauncher is synchronous: the request returns when the job has finished.
 */
@RestController
@RequestMapping("/jobs")
public class ReconciliationController {

    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;

    public ReconciliationController(JobLauncher jobLauncher, Job reconciliationJob) {
        this.jobLauncher = jobLauncher;
        this.reconciliationJob = reconciliationJob;
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, Object>> reconcile(@RequestParam LocalDate businessDate) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("businessDate", businessDate.toString())
                .toJobParameters();
        try {
            JobExecution execution = jobLauncher.run(reconciliationJob, params);
            return ResponseEntity.ok(Map.of(
                    "businessDate", businessDate.toString(),
                    "executionId", execution.getId(),
                    "status", execution.getStatus().toString()));
        } catch (JobInstanceAlreadyCompleteException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "businessDate", businessDate.toString(),
                    "message", "This business date is already reconciled"));
        }
    }
}
