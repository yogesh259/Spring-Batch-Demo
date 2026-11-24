package com.example.reconciliation.batch;

import com.example.reconciliation.domain.BankTransaction;
import com.example.reconciliation.domain.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/** Records every skipped transaction so it can be investigated. */
@Component
public class ReconciliationSkipListener implements SkipListener<BankTransaction, ReconciliationResult> {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationSkipListener.class);

    @Override
    public void onSkipInProcess(BankTransaction txn, Throwable error) {
        log.warn("Skipped {}: {}", txn.getReference(), error.getMessage());
    }
}
