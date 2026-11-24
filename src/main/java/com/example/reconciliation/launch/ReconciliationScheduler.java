package com.example.reconciliation.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Runs the job every night (cron and zone come from application.properties).
 * Turn it off with reconciliation.scheduler.enabled=false, e.g. when an external
 * scheduler (Kubernetes CronJob) starts the job instead.
 */
@Component
@ConditionalOnProperty(name = "reconciliation.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliationScheduler {

    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;
    private final ZoneId zone;

    public ReconciliationScheduler(JobLauncher jobLauncher, Job reconciliationJob,
                                   @Value("${reconciliation.zone}") String zone) {
        this.jobLauncher = jobLauncher;
        this.reconciliationJob = reconciliationJob;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${reconciliation.cron}", zone = "${reconciliation.zone}")
    public void runNightly() throws Exception {
        // Reconcile yesterday. A new date every night = a new run every night;
        // the same date again = resume that night's run if it failed.
        JobParameters params = new JobParametersBuilder()
                .addString("businessDate", LocalDate.now(zone).minusDays(1).toString())
                .toJobParameters();
        jobLauncher.run(reconciliationJob, params);
    }
}
