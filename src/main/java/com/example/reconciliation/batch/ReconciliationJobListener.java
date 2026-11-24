package com.example.reconciliation.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/** afterJob() runs whether the job succeeded or failed: the right place for alerts. */
@Component
public class ReconciliationJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Reconciliation starting with {}", jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Reconciliation finished: {}", jobExecution.getStatus());
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            // In real life: alert the on-call team (email, Slack, PagerDuty)
            log.error("Failures: {}", jobExecution.getAllFailureExceptions());
        }
    }
}
