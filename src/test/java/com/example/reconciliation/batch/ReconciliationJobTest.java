package com.example.reconciliation.batch;

import com.example.reconciliation.domain.ReconciliationResult;
import com.example.reconciliation.repository.ReconciliationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the whole job against an in-memory H2 database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "reconciliation.scheduler.enabled=false"
})
@SpringBatchTest
class ReconciliationJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private ReconciliationResultRepository resultRepository;

    @BeforeEach
    void clean() {
        jobRepositoryTestUtils.removeJobExecutions();
        resultRepository.deleteAll();
    }

    @Test
    void reconcilesOneBusinessDate() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(params("2026-11-24"));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 7 transactions: 6 reconciled, TXN-007 (no amount) skipped
        assertThat(resultRepository.count()).isEqualTo(6);
        assertThat(statusOf("TXN-001")).isEqualTo(ReconciliationResult.MATCHED);
        assertThat(statusOf("TXN-002")).isEqualTo(ReconciliationResult.AMOUNT_MISMATCH);
        assertThat(statusOf("TXN-003")).isEqualTo(ReconciliationResult.MISSING_IN_LEDGER);

        long skipped = execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("reconcileStep:"))   // one per partition
                .mapToLong(StepExecution::getProcessSkipCount)
                .sum();
        assertThat(skipped).isEqualTo(1);
    }

    @Test
    void runsOncePerBranch() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(params("2026-11-24"));

        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .contains("reconcileStep:branch-NORTH",
                          "reconcileStep:branch-SOUTH",
                          "reconcileStep:branch-EAST");
    }

    private static JobParameters params(String businessDate) {
        return new JobParametersBuilder()
                .addString("businessDate", businessDate)
                .toJobParameters();
    }

    private String statusOf(String reference) {
        return resultRepository.findById(reference).orElseThrow().getStatus();
    }
}
