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

@Configuration
public class ReconciliationJobConfig {

    /** Used as both chunk size and reader page size, so each chunk reads exactly one page. */
    @Value("${batch.chunk-size}")
    private int chunkSize;

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

    /** Step-scoped: one reader per partition, bound to that partition's branch and the job's business date. */
    @Bean
    @StepScope
    public JpaPagingItemReader<BankTransaction> reader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{jobParameters['businessDate']}") String businessDate,
            @Value("#{stepExecutionContext['branch']}") String branch) {
        return new JpaPagingItemReaderBuilder<BankTransaction>()
                .name("bankTransactionReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                        SELECT t FROM BankTransaction t
                        WHERE t.txnDate = :date AND t.branch = :branch
                        ORDER BY t.reference
                        """)                                // stable order: paging and restart depend on it
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
                .build();                                   // merge(): a rerun overwrites existing results
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
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(InvalidTransactionException.class)
                .skipLimit(10)
                .listener(skipListener)
                .build();
    }

    /** One partition per branch: "branch-NORTH" -> {branch=NORTH}. */
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
     * Fixed-size pool for the partitions; keep it below the DB connection pool size.
     * Not a default candidate, so it does not replace Spring Boot's auto-configured executor.
     */
    @Bean(defaultCandidate = false)
    public ThreadPoolTaskExecutor partitionTaskExecutor(@Value("${batch.partition-threads}") int threads) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("branch-");
        return executor;
    }

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

    @Bean
    public Step summaryStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            SummaryTasklet summaryTasklet) {
        return new StepBuilder("summaryStep", jobRepository)
                .tasklet(summaryTasklet, transactionManager)
                .build();
    }
}
