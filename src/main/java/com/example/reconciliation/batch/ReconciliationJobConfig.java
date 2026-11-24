package com.example.reconciliation.batch;

import com.example.reconciliation.domain.BankTransaction;
import com.example.reconciliation.domain.ReconciliationResult;
import com.example.reconciliation.repository.BankTransactionRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * The whole job:
 *
 *   reconciliationJob
 *     1. reconcileManagerStep  -> runs reconcileStep once per branch, in parallel
 *          reconcileStep       -> reader -> processor -> writer, in chunks
 *     2. summaryStep           -> tasklet: count results by status
 */
@Configuration
public class ReconciliationJobConfig {

    /** Reader page size and step chunk size, kept equal. 3 for the demo, ~1000 in production. */
    @Value("${batch.chunk-size}")
    private int chunkSize;

    // ---------------------------------------------------------------- job

    @Bean
    public Job reconciliationJob(JobRepository jobRepository,
                                 Step reconcileManagerStep,
                                 Step summaryStep,
                                 ReconciliationJobListener listener) {
        return new JobBuilder("reconciliationJob", jobRepository)
                .start(reconcileManagerStep)
                .next(summaryStep)
                .listener(listener)
                .build();
    }

    // ---------------------------------------------------------------- step 1: reconcile

    /**
     * Reads one branch's transactions for one business date.
     * {@code @StepScope}: a new reader per partition, created when that partition starts,
     * so it can receive the job parameter and its own branch (late binding).
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<BankTransaction> reader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{jobParameters['businessDate']}") String businessDate,
            @Value("#{stepExecutionContext['branch']}") String branch) {
        return new JpaPagingItemReaderBuilder<BankTransaction>()
                .name("bankTransactionReader")              // key for its saved position (restart)
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                        SELECT t FROM BankTransaction t
                        WHERE t.txnDate = :date AND t.branch = :branch
                        ORDER BY t.reference
                        """)                                // stable order: required for restart
                .parameterValues(Map.of(
                        "date", LocalDate.parse(businessDate),
                        "branch", branch))
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public JpaItemWriter<ReconciliationResult> writer(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<ReconciliationResult>()
                .entityManagerFactory(entityManagerFactory)
                .build();                                   // merge(): a rerun overwrites, never duplicates
    }

    @Bean
    public Step reconcileStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              JpaPagingItemReader<BankTransaction> reader,
                              ReconciliationProcessor processor,
                              JpaItemWriter<ReconciliationResult> writer,
                              ReconciliationSkipListener skipListener) {
        return new StepBuilder("reconcileStep", jobRepository)
                .<BankTransaction, ReconciliationResult>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retry(TransientDataAccessException.class) // temporary DB errors: try again
                .retryLimit(3)
                .skip(InvalidTransactionException.class)   // bad data: log it and move on
                .skipLimit(10)                             // more than 10 bad rows: fail the step
                .listener(skipListener)
                .build();
    }

    // ---------------------------------------------------------------- partitioning

    /** One partition per bank branch: {branch-NORTH -> {branch=NORTH}, ...}. */
    @Bean
    public Partitioner branchPartitioner(BankTransactionRepository transactionRepository) {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();
            for (String branch : transactionRepository.findAllBranches()) {
                ExecutionContext context = new ExecutionContext();
                context.putString("branch", branch);
                partitions.put("branch-" + branch, context);
            }
            return partitions;
        };
    }

    /**
     * Fixed-size pool for the partitions. Keep it smaller than the database connection pool.
     * defaultCandidate = false: used only where asked for by name, so it does not replace
     * Spring Boot's own task executor.
     */
    @Bean(defaultCandidate = false)
    public ThreadPoolTaskExecutor partitionTaskExecutor(@Value("${batch.partition-threads}") int threads) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("branch-");
        return executor;
    }

    /** Processes no data itself: hands out partitions and waits for all of them. */
    @Bean
    public Step reconcileManagerStep(JobRepository jobRepository,
                                     Partitioner branchPartitioner,
                                     Step reconcileStep,
                                     @Qualifier("partitionTaskExecutor") ThreadPoolTaskExecutor partitionTaskExecutor) {
        return new StepBuilder("reconcileManagerStep", jobRepository)
                .partitioner("reconcileStep", branchPartitioner)
                .step(reconcileStep)
                .taskExecutor(partitionTaskExecutor)
                .build();
    }

    // ---------------------------------------------------------------- step 2: summary

    @Bean
    public Step summaryStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            SummaryTasklet summaryTasklet) {
        return new StepBuilder("summaryStep", jobRepository)
                .tasklet(summaryTasklet, transactionManager)
                .build();
    }
}
