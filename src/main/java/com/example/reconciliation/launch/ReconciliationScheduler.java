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

/** Nightly trigger. Disable with reconciliation.scheduler.enabled=false when an external scheduler runs the job. */
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
        // One JobInstance per business date: a failed date resumes, a completed one is not rerun.
        JobParameters params = new JobParametersBuilder()
                .addString("businessDate", LocalDate.now(zone).minusDays(1).toString())
                .toJobParameters();
        jobLauncher.run(reconciliationJob, params);
    }
}
