package com.example.reconciliation.batch;

import com.example.reconciliation.repository.ReconciliationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/** One action, not "read many, write many": count the results by status. */
@Component
public class SummaryTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SummaryTasklet.class);

    private final ReconciliationResultRepository resultRepository;

    public SummaryTasklet(ReconciliationResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        resultRepository.countByStatus().forEach(count ->
                log.info("{}: {}", count.getStatus(), count.getTotal()));
        return RepeatStatus.FINISHED;
    }
}
